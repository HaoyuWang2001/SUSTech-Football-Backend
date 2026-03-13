package com.sustech.football.service.impl;

import com.github.jeffreyning.mybatisplus.service.MppServiceImpl;
import com.sustech.football.entity.EventTeamRequest;
import com.sustech.football.mapper.EventTeamRequestMapper;
import com.sustech.football.service.EventTeamRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventTeamRequestServiceImpl extends MppServiceImpl<EventTeamRequestMapper, EventTeamRequest> implements EventTeamRequestService {

    private final EventTeamRequestMapper eventTeamRequestMapper;

    @Autowired
    public EventTeamRequestServiceImpl(EventTeamRequestMapper eventTeamRequestMapper) {
        this.eventTeamRequestMapper = eventTeamRequestMapper;
    }

    @Override
    public List<EventTeamRequest> listWithEvent(Long teamId, String type) {
        return baseMapper.selectListWithEvent(teamId, type);
    }

    @Override
    public List<EventTeamRequest> listWithTeam(Long eventId, String type) {
        return baseMapper.selectListWithTeam(eventId, type);
    }

}
