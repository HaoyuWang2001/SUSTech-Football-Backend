
package com.sustech.football.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sustech.football.entity.*;
import com.sustech.football.exception.BadRequestException;
import com.sustech.football.exception.ConflictException;
import com.sustech.football.exception.InternalServerErrorException;
import com.sustech.football.exception.ResourceNotFoundException;
import com.sustech.football.mapper.*;
import com.sustech.football.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class MatchServiceImpl extends ServiceImpl<MatchMapper, Match> implements MatchService {
    @Autowired
    private MatchManagerService matchManagerService;
    @Autowired
    private MatchRefereeService matchRefereeService;
    @Autowired
    private MatchRefereeRequestService matchRefereeRequestService;
    @Autowired
    private MatchTeamRequestService matchTeamRequestService;
    @Autowired
    private MatchPlayerActionService matchPlayerActionService;
    @Autowired
    private TeamService teamService;
    @Autowired
    private TeamPlayerService teamPlayerService;
    @Autowired
    private EventMatchService eventMatchService;
    @Autowired
    private EventService eventService;
    @Autowired
    private EventMatchMapper eventMatchMapper;
    @Autowired
    private EventGroupMapper eventGroupMapper;
    @Autowired
    private EventGroupTeamMapper eventGroupTeamMapper;
    @Autowired
    private MatchCreatorMapper matchCreatorMapper;


    @Override
    public Match getMatch(Long matchId) {
        Match match = getById(matchId);
        if (match == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        if (match.getHomeTeamId() != null) {
            Team homeTeam = teamService.getById(match.getHomeTeamId());
            homeTeam.setPlayerList(teamPlayerService.listWithPlayer(homeTeam.getTeamId()).stream().map(TeamPlayer::getPlayer).toList());
            match.setHomeTeam(homeTeam);
        }
        if (match.getAwayTeamId() != null) {
            Team awayTeam = teamService.getById(match.getAwayTeamId());
            awayTeam.setPlayerList(teamPlayerService.listWithPlayer(awayTeam.getTeamId()).stream().map(TeamPlayer::getPlayer).toList());
            match.setAwayTeam(awayTeam);
        }
        match.setRefereeList(matchRefereeService.listWithReferee(matchId)
                .stream()
                .map(MatchReferee::getReferee)
                .toList());
        match.setMatchPlayerActionList(matchPlayerActionService.list(new QueryWrapper<MatchPlayerAction>().eq("match_id", matchId)));
        match.setMatchEvent(this.findMatchEvent(match));
        return match;
    }

    @Override
    public List<Match> getMatchByIdList(List<Long> matchIdList) {
        return matchIdList.stream()
                .map(this::getMatch)
                .sorted(Comparator.comparing(Match::getTime))
                .toList();
    }

    @Override
    public MatchEvent findMatchEvent(Match match) {
        return new MatchEvent(eventMatchService.getEventMatchByMatch(match.getMatchId()));
    }

    public List<Match> getAllMatches() {
        List<Match> matches = list();
        for (Match match : matches) {
            if (match.getHomeTeamId() != null) {
                Team homeTeam = teamService.getById(match.getHomeTeamId());
                match.setHomeTeam(homeTeam);
            }
            if (match.getAwayTeamId() != null) {
                Team awayTeam = teamService.getById(match.getAwayTeamId());
                match.setAwayTeam(awayTeam);
            }
        }

        return matches;
    }

    @Override
    @Transactional
    public boolean deleteMatch(Long matchId, Long userId) {
        if (userId != 0) {
            QueryWrapper<MatchManager> matchManagerQueryWrapper = new QueryWrapper<>();
            matchManagerQueryWrapper.eq("match_id", matchId).eq("user_id", userId);
            List<MatchManager> managers = matchManagerService.list(matchManagerQueryWrapper);
            if (managers == null) {
                throw new BadRequestException("用户不是该比赛的管理员，无法删除比赛");
            }
        }

        QueryWrapper<EventMatch> eventMatchQueryWrapper = new QueryWrapper<>();
        eventMatchQueryWrapper.eq("match_id", matchId);
        List<EventMatch> eventMatch = eventMatchService.list(eventMatchQueryWrapper);
        if (!eventMatch.isEmpty()) {
            throw new BadRequestException("赛事比赛，无法删除");
        }

        MatchCreator matchCreator = new MatchCreator();
        matchCreator.setMatchId(matchId);
        matchCreatorMapper.deleteById(matchCreator);

        if (!removeById(matchId)) {
            throw new RuntimeException("删除比赛失败");
        }
        return true;
    }

    @Override
    public boolean inviteManager(MatchManager matchManager) {
        if (matchManagerService.selectByMultiId(matchManager) != null) {
            throw new ConflictException("该用户已经是该比赛的管理员");
        }
        if (!matchManagerService.saveOrUpdateByMultiId(matchManager)) {
            throw new RuntimeException("邀请失败");
        }
        return true;
    }

    @Override
    public List<Long> getManagers(Long matchId) {
        QueryWrapper<MatchManager> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId);
        return matchManagerService.list(queryWrapper).stream().map(MatchManager::getUserId).toList();
    }

    @Override
    public boolean deleteManager(MatchManager matchManager) {
        if (!matchManagerService.deleteByMultiId(matchManager)) {
            throw new BadRequestException("删除管理员失败");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean inviteTeam(MatchTeamRequest matchTeamRequest) {
        Long matchId = matchTeamRequest.getMatchId();
        Match match = getById(matchId);
        List<MatchTeamRequest> matchTeamRequestList = matchTeamRequestService.listWithTeam(matchId);
        if (matchTeamRequest.getType().equals(MatchTeamRequest.TYPE_HOME)) {
            if (match.getHomeTeamId() != null) {
                throw new ConflictException("主队已存在");
            }
            if (match.getAwayTeamId() != null && match.getAwayTeamId().equals(matchTeamRequest.getTeamId())) {
                throw new ConflictException("球队是客队");
            }
            if (matchTeamRequestList.stream().anyMatch(request -> request.getTeamId().equals(matchTeamRequest.getTeamId()))) {
                throw new ConflictException("球队已邀请");
            }
            if (matchTeamRequestList.stream().anyMatch(request -> request.getType().equals(MatchTeamRequest.TYPE_HOME) && request.getStatus().equals(MatchTeamRequest.STATUS_PENDING))) {
                throw new ConflictException("已邀请主队");
            }
            matchTeamRequestService.saveOrUpdateByMultiId(matchTeamRequest);
        } else if (matchTeamRequest.getType().equals(MatchTeamRequest.TYPE_AWAY)) {
            if (match.getAwayTeamId() != null) {
                throw new ConflictException("客队已存在");
            }
            if (match.getHomeTeamId() != null && match.getHomeTeamId().equals(matchTeamRequest.getTeamId())) {
                throw new ConflictException("球队是主队");
            }
            if (matchTeamRequestList.stream().anyMatch(request -> request.getTeamId().equals(matchTeamRequest.getTeamId()))) {
                throw new ConflictException("球队已邀请");
            }
            if (matchTeamRequestList.stream().anyMatch(request -> request.getType().equals(MatchTeamRequest.TYPE_AWAY) && request.getStatus().equals(MatchTeamRequest.STATUS_PENDING))) {
                throw new ConflictException("已邀请客队");
            }
            matchTeamRequestService.saveOrUpdateByMultiId(matchTeamRequest);
        } else {
            throw new BadRequestException("邀请类型错");
        }
        return true;
    }

    @Override
    public List<MatchTeamRequest> getTeamInvitations(Long matchId) {
        return matchTeamRequestService.listWithTeam(matchId);
    }

    @Override
    public boolean deleteTeam(Long matchId, Boolean isHomeTeam) {
        Match match = getById(matchId);
        if (match == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        if (isHomeTeam) {
            if (match.getHomeTeamId() == null) {
                throw new BadRequestException("主队不存在");
            }
            match.setHomeTeamId(null);
        } else {
            if (match.getAwayTeamId() == null) {
                throw new BadRequestException("客队不存在");
            }
            match.setAwayTeamId(null);
        }
        if (!updateById(match)) {
            throw new BadRequestException("删除队伍失败");
        }
        return false;
    }

    @Override
    public boolean inviteReferee(MatchReferee matchReferee) {
        if (matchRefereeService.selectByMultiId(matchReferee) != null) {
            throw new ConflictException("该用户已经是该比赛的裁判");
        }
        MatchRefereeRequest matchRefereeRequest = new MatchRefereeRequest();
        matchRefereeRequest.setMatchId(matchReferee.getMatchId());
        matchRefereeRequest.setRefereeId(matchReferee.getRefereeId());
        matchRefereeRequest.setStatus(MatchRefereeRequest.STATUS_PENDING);

        MatchRefereeRequest requestRes = matchRefereeRequestService.selectByMultiId(matchRefereeRequest);
        if (requestRes != null && requestRes.getStatus().equals(MatchRefereeRequest.STATUS_PENDING)) {
            throw new ConflictException("已经邀请过该裁判，请勿重复发送");
        }
        if (!matchRefereeRequestService.saveOrUpdateByMultiId(matchRefereeRequest)) {
            throw new RuntimeException("邀请裁判失败");
        }
        return true;
    }

    @Override
    public List<Referee> getReferees(Long matchId) {
        return matchRefereeService.listWithReferee(matchId)
                .stream()
                .map(MatchReferee::getReferee)
                .toList();
    }

    @Override
    public boolean deleteReferee(MatchReferee matchReferee) {
        if (matchRefereeService.selectByMultiId(matchReferee) == null) {
            throw new ResourceNotFoundException("比赛未含有该裁判，删除失败");
        }
        if (!matchRefereeService.deleteByMultiId(matchReferee)) {
            throw new RuntimeException("删除裁判失败");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean updateResult(Long refereeId, Match match) {
        List<Long> refereeList = matchRefereeService
                .listWithReferee(match.getMatchId())
                .stream()
                .map(MatchReferee::getRefereeId)
                .toList();
        if (!refereeList.contains(refereeId)) {
            throw new BadRequestException("裁判不在本场比赛的执法队列中，无法修改结果");
        }
        if (!updateById(match)) {
            throw new RuntimeException("修改比赛结果失败");
        }
        // 需要先获取eventMatch
        EventMatch eventMatch = eventMatchMapper.selectOne(new QueryWrapper<EventMatch>().eq("match_id", match.getMatchId()));
        if (eventMatch != null && eventMatch.getStage().equals(EventMatch.STAGE_CUP_GROUP)) { // 若该match属于某个event，且是一场小组赛（CUP_GROUP)
            List<EventGroup> eventGroupList = eventGroupMapper.selectList(new QueryWrapper<EventGroup>().eq("event_id", eventMatch.getEventId()));
            EventGroupTeam eventGroupTeam_home = new EventGroupTeam();
            EventGroupTeam eventGroupTeam_away = new EventGroupTeam();
            for (EventGroup eventGroup : eventGroupList) {
                Long groupId = eventGroup.getGroupId();
                Long homeTeamId = match.getHomeTeamId();
                eventGroupTeam_home = eventGroupTeamMapper.selectOne(new QueryWrapper<EventGroupTeam>().eq("group_id", groupId).eq("team_id", homeTeamId));
                if (eventGroupTeam_home != null) {
                    break;
                }
            }
            for (EventGroup eventGroup : eventGroupList) {
                Long groupId = eventGroup.getGroupId();
                Long awayTeamId = match.getAwayTeamId();
                eventGroupTeam_away = eventGroupTeamMapper.selectOne(new QueryWrapper<EventGroupTeam>().eq("group_id", groupId).eq("team_id", awayTeamId));
                if (eventGroupTeam_away != null) {
                    break;
                }
            }
            if (eventGroupTeam_home == null || eventGroupTeam_away == null) {
                throw new InternalServerErrorException("修改比赛结果失败，异常错误");
            }
            if (!Objects.equals(eventGroupTeam_home.getGroupId(), eventGroupTeam_away.getGroupId())) {
                throw new BadRequestException("球队不在同一小组，无法修改比赛结果");
            }

            // 小组赛不考虑点球
            // 更新小组赛胜/平/负场数
            if (match.getHomeTeamScore() > match.getAwayTeamScore()) {
                eventGroupTeam_home.setNumWins(eventGroupTeam_home.getNumWins() + 1);
                eventGroupTeam_away.setNumLosses(eventGroupTeam_away.getNumLosses() + 1);
            } else if (match.getHomeTeamScore() < match.getAwayTeamScore()) {
                eventGroupTeam_home.setNumLosses(eventGroupTeam_home.getNumLosses() + 1);
                eventGroupTeam_away.setNumWins(eventGroupTeam_away.getNumWins() + 1);
            } else {
                eventGroupTeam_home.setNumDraws(eventGroupTeam_home.getNumDraws() + 1);
                eventGroupTeam_away.setNumDraws(eventGroupTeam_away.getNumDraws() + 1);
            }

            // 更新小组赛进/失球数
            eventGroupTeam_home.setNumGoalsFor(eventGroupTeam_home.getNumGoalsFor() + match.getHomeTeamScore());
            eventGroupTeam_home.setNumGoalsAgainst(eventGroupTeam_home.getNumGoalsAgainst() + match.getAwayTeamScore());
            eventGroupTeam_away.setNumGoalsFor(eventGroupTeam_away.getNumGoalsFor() + match.getAwayTeamScore());
            eventGroupTeam_away.setNumGoalsAgainst(eventGroupTeam_away.getNumGoalsAgainst() + match.getHomeTeamScore());

            // 更新小组积分
            eventGroupTeam_home.setScore(eventGroupTeam_home.getNumWins() * 3 + eventGroupTeam_home.getNumDraws());
            eventGroupTeam_away.setScore(eventGroupTeam_away.getNumWins() * 3 + eventGroupTeam_away.getNumDraws());

            // 持久化更新后的分组积分与统计
            if (eventGroupTeamMapper.update(eventGroupTeam_home, new QueryWrapper<EventGroupTeam>()
                    .eq("group_id", eventGroupTeam_home.getGroupId())
                    .eq("team_id", eventGroupTeam_home.getTeamId())) < 1) {
                throw new InternalServerErrorException("更新小组赛积分失败（主队）");
            }
            if (eventGroupTeamMapper.update(eventGroupTeam_away, new QueryWrapper<EventGroupTeam>()
                    .eq("group_id", eventGroupTeam_away.getGroupId())
                    .eq("team_id", eventGroupTeam_away.getTeamId())) < 1) {
                throw new InternalServerErrorException("更新小组赛积分失败（客队）");
            }
        }
        return true;
    }

    @Override
    public boolean addPlayerAction(Long refereeId, MatchPlayerAction matchPlayerAction) {
        List<Long> refereeList = matchRefereeService
                .listWithReferee(matchPlayerAction.getMatchId())
                .stream()
                .map(MatchReferee::getRefereeId)
                .toList();
        if (!refereeList.contains(refereeId)) {
            throw new BadRequestException("裁判不在本场比赛的执法队列中，无法修改结果");
        }
        if (!matchPlayerActionService.saveOrUpdateByMultiId(matchPlayerAction)) {
            throw new RuntimeException("增加本场比赛的球员行为失败");
        }
        return true;
    }

    @Override
    public Event getEvent(Long matchId) {
        EventMatch eventMatch = eventMatchService.getOne(new QueryWrapper<EventMatch>().eq("match_id", matchId));
        return eventService.getDetailedEvent(eventMatch.getEventId());
    }

    @Override
    public List<MatchPlayerAction> getMatchPlayerActions(Long matchId) {
        return matchPlayerActionService.list(new QueryWrapper<MatchPlayerAction>().eq("match_id", matchId));
    }

    @Override
    public boolean deletePlayerAction(Long refereeId, MatchPlayerAction matchPlayerAction) {
        List<Long> refereeList = matchRefereeService
                .listWithReferee(matchPlayerAction.getMatchId())
                .stream()
                .map(MatchReferee::getRefereeId)
                .toList();
        if (!refereeList.contains(refereeId)) {
            throw new BadRequestException("裁判不在本场比赛的执法队列中，无法修改结果");
        }
        if (!matchPlayerActionService.deleteByMultiId(matchPlayerAction)) {
            throw new RuntimeException("增加本场比赛的球员行为失败");
        }
        return true;
    }
}