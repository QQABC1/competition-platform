package com.platform.registration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.registration.dto.RegistrationAuditDTO;
import com.platform.registration.dto.RegistrationDTO;
import com.platform.registration.dto.RegistrationQueryDTO;
import com.platform.registration.entity.Registration;
import com.platform.registration.vo.RegistrationInitVO;
import com.platform.registration.vo.RegistrationListVO;
import com.platform.registration.vo.RegistrationStatusVO;

/**
 * 报名服务接口
 */
public interface RegistrationService extends IService<Registration> {

    /**
     * 获取报名初始化信息
     * @param userId 当前用户ID
     * @param competitionId 竞赛ID
     * @return 初始化VO (含报名状态和用户信息)
     */
    RegistrationInitVO getInitInfo(Long userId, Long competitionId);

    /**
     * 提交报名
     * @param userId 当前用户ID
     * @param dto 提交的表单数据
     */
    void apply(Long userId, RegistrationDTO.Apply dto);

    // 分页查询报名名单
    IPage<RegistrationListVO> getOrganizerList(RegistrationQueryDTO queryDTO);

    // 审核报名
    void auditRegistration(RegistrationAuditDTO dto);

    /**
     * 高并发提交报名 (异步削峰)
     * @param userId
     * @param dto
     */
    void applyAsync(Long userId, RegistrationDTO.Apply dto);

    /**
     * 前端轮询查询报名结果
     * @param userId
     * @param compId
     * @return
     */
    RegistrationStatusVO getApplyStatus(Long userId, Long compId);
}