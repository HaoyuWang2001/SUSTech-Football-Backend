package com.sustech.football.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sustech.football.entity.EventTeamRoster;
import com.sustech.football.entity.Player;

import java.util.List;

public interface EventTeamRosterService extends IService<EventTeamRoster> {
    /**
     * 设置球队在赛事中的大名单
     * @param eventId 赛事ID
     * @param teamId 球队ID
     * @param playerIds 球员ID列表
     * @return 是否成功
     */
    boolean setRoster(Long eventId, Long teamId, List<Long> playerIds);

    /**
     * 获取球队在赛事中的大名单
     * @param eventId 赛事ID
     * @param teamId 球队ID
     * @return 大名单球员列表
     */
    List<Player> getRoster(Long eventId, Long teamId);

    /**
     * 检查球队是否已经设置大名单
     * @param eventId 赛事ID
     * @param teamId 球队ID
     * @return 是否已设置
     */
    boolean hasSetRoster(Long eventId, Long teamId);

    /**
     * 更新球员在大名单中的号码
     * @param eventId 赛事ID
     * @param teamId 球队ID
     * @param playerId 球员ID
     * @param number 号码
     * @return 是否成功
     */
    boolean updatePlayerNumber(Long eventId, Long teamId, Long playerId, Integer number);
}
