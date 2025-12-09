package com.platform.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.api.client.RemoteUserService;
import com.platform.api.domain.UserInternalVO;
import com.platform.common.api.R;
import com.platform.common.exception.BusinessException;
import com.platform.registration.dto.RegistrationDTO;
import com.platform.registration.entity.Registration;
import com.platform.registration.mapper.RegistrationMapper;
import com.platform.registration.service.RegistrationService;
import com.platform.registration.vo.RegistrationInitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration> implements RegistrationService {

    /**
     * 注入 api 模块定义的 Feign 接口
     * 记得在启动类添加 @EnableFeignClients(basePackages = "com.platform.api.client")
     */
    @Autowired
    private RemoteUserService remoteUserService;

    /**
     * 获取报名初始化信息 (Pre-check)
     * 1. 检查是否已报名
     * 2. 回显用户信息 (远程调用)
     */
    @Override
    public RegistrationInitVO getInitInfo(Long userId, Long competitionId) {
        RegistrationInitVO vo = new RegistrationInitVO();

        // ---------------------------------------------------------
        // 步骤 1: 查询本地数据库，检查该用户对该比赛的报名状态
        // ---------------------------------------------------------
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, userId)
                .eq(Registration::getCompetitionId, competitionId)
                .eq(Registration::getStatus, 1); // 1 表示有效报名

        Registration registration = this.getOne(wrapper);

        if (registration != null) {
            vo.setIsRegistered(true);
            vo.setRegistrationId(registration.getId());
        } else {
            vo.setIsRegistered(false);
            vo.setRegistrationId(null);
        }

        // ---------------------------------------------------------
        // 步骤 2: 远程调用 User 服务，获取学生基础信息用于回显
        // ---------------------------------------------------------
        try {
            R<UserInternalVO> userResp = remoteUserService.getUserInfo(userId);

            if (userResp != null && userResp.getCode() == 200 && userResp.getData() != null) {
                UserInternalVO user = userResp.getData();

                // 数据回填
                vo.setStudentName(user.getRealName());
                vo.setStudentId(user.getStudentId());
                vo.setMajor(user.getMajor());
                vo.setPhone(user.getPhone());

                // 判断信息是否完善 (假设 1 为已完善)
                // 如果未完善，前端会据此弹窗提示去绑定信息
                vo.setIsProfileComplete(user.getIsInfoComplete() != null && user.getIsInfoComplete() == 1);
            } else {
                // 如果调用成功但业务返回失败，或数据为空
                log.warn("获取用户信息失败或为空, userId: {}", userId);
                vo.setIsProfileComplete(false);
            }
        } catch (Exception e) {
            // 远程调用如果挂了（比如 User 服务没启动），这里做一个容错处理
            log.error("远程调用用户服务异常", e);
            vo.setIsProfileComplete(false);
        }

        return vo;
    }

    /**
     * 提交报名 (实现参考)
     */
    @Override
    public void apply(Long userId, RegistrationDTO.Apply dto) {
        // 1. 幂等性检查
        long count = this.count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .eq(Registration::getCompetitionId, dto.getCompetitionId())
                .eq(Registration::getStatus, 1));

        if (count > 0) {
            throw new BusinessException("您已报名该竞赛，请勿重复提交");
        }

        // 2. 数据转换与入库
        Registration reg = new Registration();
        BeanUtils.copyProperties(dto, reg);
        reg.setUserId(userId);
        reg.setStatus(1); // 1: 已报名
        // 注意：DTO 中的 attachmentUrl 对应 Entity 中的 attachment
        reg.setAttachment(dto.getAttachmentUrl());

        this.save(reg);
    }
}