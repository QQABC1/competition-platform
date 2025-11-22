package com.platform.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.utils.JwtUtils;
import com.platform.user.dto.UserAuthDTO;
import com.platform.user.entity.User;
import com.platform.user.mapper.UserMapper;
import com.platform.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String SMS_PREFIX = "auth:sms:";

    @Override
    public void sendCode(String phone) {
        // 1. 简单的手机号校验 (生产环境可用正则)
        if (!StringUtils.hasText(phone) || phone.length() != 11) {
            throw new RuntimeException("手机号格式不正确");
        }

        // 2. 生成6位随机数
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));

        // 3. 存入Redis，5分钟有效
        redisTemplate.opsForValue().set(SMS_PREFIX + phone, code, 5, TimeUnit.MINUTES);

        // 4. 模拟发送短信 (实际对接阿里云/腾讯云短信API)
        System.out.println(">>> 【模拟短信】发送给 " + phone + " 的验证码是: " + code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAuthDTO.LoginResponse login(UserAuthDTO.LoginRequest request) {
        String phone = request.getPhone();

        // 1. 校验验证码
        String cacheCode = redisTemplate.opsForValue().get(SMS_PREFIX + phone);
        if (cacheCode == null || !cacheCode.equals(request.getCode())) {
            throw new RuntimeException("验证码错误或已失效");
        }

        // 2. 查询数据库是否存在该手机号
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));

        boolean isNewUser = false;
        if (user == null) {
            // 3. 不存在则自动注册
            user = new User();
            user.setPhone(phone);
            user.setIsInfoComplete(0); // 未完善
            this.save(user); // MyBatis-Plus 插入并回填ID
            isNewUser = true;
        }

        // 4. 生成 Token
        String token = JwtUtils.createToken(user.getId());

        // 5. 封装返回结果
        UserAuthDTO.LoginResponse response = new UserAuthDTO.LoginResponse();
        response.setToken(token);
        // 如果是新用户 或者 数据库标记未完善，则需要绑定
        response.setNeedBinding(isNewUser || user.getIsInfoComplete() == 0);

        // 6. 删除验证码 (防止重复使用)
        redisTemplate.delete(SMS_PREFIX + phone);

        return response;
    }

    @Override
    public void bindUserInfo(Long userId, UserAuthDTO.BindRequest request) {
        // 1. 检查学号是否已被占用 (除自己外)
        Long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getStudentId, request.getStudentId())
                .ne(User::getId, userId)); // ID不等于自己

        if (count > 0) {
            throw new RuntimeException("该学号已被绑定，请联系管理员");
        }

        // 2. 更新用户信息
        User user = new User();
        user.setId(userId);
        user.setStudentId(request.getStudentId());
        user.setRealName(request.getRealName());
        user.setMajor(request.getMajor());
        user.setIsInfoComplete(1); // 标记为已完善

        this.updateById(user);
    }
}