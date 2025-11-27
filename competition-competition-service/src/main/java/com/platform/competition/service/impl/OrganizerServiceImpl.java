package com.platform.competition.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.competition.entity.Organizer;
import com.platform.competition.mapper.OrganizerMapper;
import com.platform.competition.service.OrganizerService;
import org.springframework.stereotype.Service;

@Service
public class OrganizerServiceImpl extends ServiceImpl<OrganizerMapper, Organizer> implements OrganizerService {
}