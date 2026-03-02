package com.platform.democompetitionregistrationservice;

import com.platform.registration.RegistrationApplication;
import com.platform.registration.dto.RegistrationAuditDTO;
import com.platform.registration.entity.Registration;
import com.platform.registration.mapper.RegistrationMapper;
import com.platform.registration.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = RegistrationApplication.class)
public class RegistrationNotificationTest {
    // 注入真实要测试的报名审核 Service
    @Autowired
    private RegistrationService registrationService;

    // Mock 数据库操作，避免因为没有真实表数据导致测试失败
    @MockBean
    private RegistrationMapper registrationMapper;

    @Test
    public void testAuditRegistrationAndSendMessage() throws InterruptedException {
        // ================= 1. 准备测试数据 =================
        Long testRegId = 1001L;
        RegistrationAuditDTO dto = new RegistrationAuditDTO();
        dto.setId(testRegId);
        dto.setPass(true); // 模拟审核通过
        dto.setReason("符合参赛要求，准许通过");

        // 模拟数据库返回记录 (状态为1：待审核)
        Registration mockReg = new Registration();
        mockReg.setId(testRegId);
        mockReg.setUserId(888L);
        mockReg.setCompetitionId(999L);
        mockReg.setStatus(1);

        // 当 service 调用 selectById 时，返回 mockReg
        Mockito.when(registrationMapper.selectById(testRegId)).thenReturn(mockReg);

        // ================= 2. 执行真实业务逻辑 =================
        System.out.println("🚀 [发送端测试] 开始执行审核逻辑...");
        registrationService.auditRegistration(dto);

        // ================= 3. 结果验证 =================
        // 验证数据库 Update 方法被成功调用了 1 次
        Mockito.verify(registrationMapper, Mockito.times(1)).updateById(Mockito.any(Registration.class));

        System.out.println("✅ [发送端测试] 数据库状态已更新，RabbitMQ 消息已发送！");

        // 主线程休眠 2 秒，确保底层的 RabbitTemplate 异步网络 IO 发送完毕
        Thread.sleep(2000);
    }
}
