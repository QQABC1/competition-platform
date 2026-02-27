package com.platform.democompetitionregistrationservice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.registration.RegistrationApplication;
import com.platform.registration.dto.RegistrationDTO;
import com.platform.registration.entity.Registration;
import com.platform.registration.mapper.RegistrationMapper;
import com.platform.registration.service.RegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实环境集成测试 (不使用 Mock)
 * 前置要求：必须开启本地/测试环境的 Redis 和 MySQL 服务
 */
@SpringBootTest(classes = RegistrationApplication.class)
public class RegistrationServiceTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 测试用常量
    private final Long TEST_COMP_ID = 9999L;
    private final Long TEST_USER_ID = 8888L;

    // Redis Keys (与你业务代码中保持一致)
    private final String IDEMP_KEY = "comp:apply:idemp:" + TEST_COMP_ID + ":" + TEST_USER_ID;
    private final String CAP_KEY = "comp:capacity:" + TEST_COMP_ID;
    private final String RES_KEY = "comp:apply:result:" + TEST_COMP_ID + ":" + TEST_USER_ID;

    @BeforeEach
    public void setup() {
        // 1. 清理数据库历史脏数据
        registrationMapper.delete(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getCompetitionId, TEST_COMP_ID));

        // 2. 清理 Redis 历史数据
        redisTemplate.delete(IDEMP_KEY);
        redisTemplate.delete(CAP_KEY);
        redisTemplate.delete(RES_KEY);

        // 3. 模拟 Competition 审核通过时的“缓存预热”
        // 假设这场比赛只有 2 个名额
        redisTemplate.opsForValue().set(CAP_KEY, "2");
    }

    @AfterEach
    public void cleanup() {
        // 测试结束后打扫战场，保持环境干净
        registrationMapper.delete(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getCompetitionId, TEST_COMP_ID));
        redisTemplate.delete(IDEMP_KEY);
        redisTemplate.delete(CAP_KEY);
        redisTemplate.delete(RES_KEY);
    }

    /**
     * 测试场景 1：正常异步报名流程
     */
    @Test
    public void test1_ApplyAsync_SuccessFlow() throws InterruptedException {
        // 准备请求参数
        RegistrationDTO.Apply dto = new RegistrationDTO.Apply();
        dto.setCompetitionId(TEST_COMP_ID);
        dto.setContactPhone("13800138000");

        // 1. 发起请求
        registrationService.applyAsync(TEST_USER_ID, dto);

        // 2. 立即验证 Redis 防重 Key 是否写入 (主线程同步动作)
        assertTrue(redisTemplate.hasKey(IDEMP_KEY), "防重幂等Key应该被写入");

        // 3. 验证名额是否被扣减 (原本是2，现在应该是1)
        assertEquals("1", redisTemplate.opsForValue().get(CAP_KEY), "名额应该扣减为1");

        // 4. 等待异步线程池执行完毕 (因为没有Mock，必须让主线程等一会儿，否则测试用例结束了线程还没跑完)
        System.out.println("主线程等待异步落库...");
        TimeUnit.SECONDS.sleep(2); // 强制睡2秒等待异步任务

        // 5. 验证数据库真实落库
        Registration dbReg = registrationMapper.selectOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getCompetitionId, TEST_COMP_ID)
                .eq(Registration::getUserId, TEST_USER_ID));
        assertNotNull(dbReg, "数据库中应该存在报名记录");
        assertEquals(1, dbReg.getStatus(), "记录状态应该为待审核(1)");

        // 6. 验证结果状态 Key 是否写入 SUCCESS
        assertEquals("SUCCESS", redisTemplate.opsForValue().get(RES_KEY), "异步结果应该标记为SUCCESS");
    }

    /**
     * 测试场景 2：短时间内重复点击 (防重测试)
     */
    @Test
    public void test2_ApplyAsync_DuplicateClick() {
        RegistrationDTO.Apply dto = new RegistrationDTO.Apply();
        dto.setCompetitionId(TEST_COMP_ID);

        // 第一次点击：应该成功加入队列
        registrationService.applyAsync(TEST_USER_ID, dto);

        // 第二次疯狂连击：必须抛出异常拦截
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            registrationService.applyAsync(TEST_USER_ID, dto);
        });

        assertEquals("您的报名正在处理中或已成功，请勿重复点击", exception.getMessage());

        // 名额只应该被扣一次 (原本是2，现在应该是1，不是0)
        assertEquals("1", redisTemplate.opsForValue().get(CAP_KEY));
    }

    /**
     * 测试场景 3：名额耗尽 (防超卖测试)
     */
    @Test
    public void test3_ApplyAsync_CapacityFull() {
        RegistrationDTO.Apply dto = new RegistrationDTO.Apply();
        dto.setCompetitionId(TEST_COMP_ID);

        // 模拟名额已经被其他人抢光了 (手动将 Redis 名额改为 0)
        redisTemplate.opsForValue().set(CAP_KEY, "0");

        // 此时发起报名：必须抛出名额已满的异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            registrationService.applyAsync(TEST_USER_ID, dto);
        });

        assertEquals("报名人数已满，请留意后续名额", exception.getMessage());

        // 验证回滚补偿逻辑：
        // 1. 名额加回去，还是 0
        assertEquals("0", redisTemplate.opsForValue().get(CAP_KEY), "名额补偿恢复错误");
        // 2. 防重 Key 被删除，允许下次重试
        assertFalse(redisTemplate.hasKey(IDEMP_KEY), "防重Key应该被清除");
    }

    /**
     * 测试场景 4：高并发抢票模拟 (进阶测试)
     * 模拟 50 个人同时抢 2 个名额，测试是否会超卖
     */
    @Test
    public void test4_HighConcurrency() throws InterruptedException {
        int threads = 50;
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final Long mockUserId = (long) i; // 50个不同的用户
            executor.submit(() -> {
                try {
                    RegistrationDTO.Apply dto = new RegistrationDTO.Apply();
                    dto.setCompetitionId(TEST_COMP_ID);
                    dto.setContactPhone("13800138000");
                    registrationService.applyAsync(mockUserId, dto);
                } catch (Exception e) {
                    // 忽略被拦截的异常，只看成功的
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程发起请求完毕
        latch.await();
        // 等待异步落库完毕
        TimeUnit.SECONDS.sleep(3);

        // 最终验证：数据库里绝对只能有 2 条记录 (因为初始设置了 CAP_KEY="2")
        Long successCount = registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getCompetitionId, TEST_COMP_ID));

        System.out.println("最终成功抢到名额并落库的人数：" + successCount);
        assertEquals(2L, successCount, "发生了超卖或少卖现象！");

        // 名额必须精确扣减到 0
        assertEquals("0", redisTemplate.opsForValue().get(CAP_KEY));
    }
}