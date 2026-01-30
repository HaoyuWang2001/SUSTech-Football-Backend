package com.sustech.football.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sustech.football.entity.EventTeamRoster;
import com.sustech.football.entity.Player;
import com.sustech.football.mapper.EventTeamRosterMapper;
import com.sustech.football.service.EventTeamRosterService;
import com.sustech.football.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EventTeamRosterServiceImpl extends ServiceImpl<EventTeamRosterMapper, EventTeamRoster> implements EventTeamRosterService {

    @Autowired
    private PlayerService playerService;

    @Override
    @Transactional
    public boolean setRoster(Long eventId, Long teamId, List<Long> playerIds) {
        if (eventId == null || teamId == null || playerIds == null || playerIds.isEmpty()) {
            return false;
        }

        // 删除旧的大名单
        QueryWrapper<EventTeamRoster> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("event_id", eventId).eq("team_id", teamId);
        this.remove(queryWrapper);

        // 插入新的大名单
        List<EventTeamRoster> rosterList = new ArrayList<>();
        for (Long playerId : playerIds) {
            EventTeamRoster roster = new EventTeamRoster(eventId, teamId, playerId);
            rosterList.add(roster);
        }

        return this.saveBatch(rosterList);
    }

    @Override
    public List<Player> getRoster(Long eventId, Long teamId) {
        if (eventId == null || teamId == null) {
            return new ArrayList<>();
        }

        QueryWrapper<EventTeamRoster> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("event_id", eventId).eq("team_id", teamId);
        List<EventTeamRoster> rosterList = this.list(queryWrapper);

        List<Player> players = new ArrayList<>();
        for (EventTeamRoster roster : rosterList) {
            Player player = playerService.getById(roster.getPlayerId());
            if (player != null) {
                players.add(player);
            }
        }

        return players;
    }

    @Override
    public boolean hasSetRoster(Long eventId, Long teamId) {
        if (eventId == null || teamId == null) {
            return false;
        }

        QueryWrapper<EventTeamRoster> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("event_id", eventId).eq("team_id", teamId);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public boolean updatePlayerNumber(Long eventId, Long teamId, Long playerId, Integer number) {
        if (eventId == null || teamId == null || playerId == null) {
            return false;
        }

        QueryWrapper<EventTeamRoster> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("event_id", eventId).eq("team_id", teamId).eq("player_id", playerId);
        
        EventTeamRoster roster = this.getOne(queryWrapper);
        if (roster == null) {
            return false;
        }

        roster.setNumber(number);
        return this.updateById(roster);
    }
}
