package com.sustech.football.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sustech.football.entity.*;
import com.sustech.football.exception.ConflictException;
import com.sustech.football.exception.InternalServerErrorException;
import com.sustech.football.exception.ResourceNotFoundException;
import com.sustech.football.mapper.EventMapper;
import com.sustech.football.mapper.MatchRefereeMapper;
import com.sustech.football.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class EventServiceImpl extends ServiceImpl<EventMapper, Event> implements EventService {
    @Autowired
    private EventManagerService eventManagerService;
    @Autowired
    private EventTeamService eventTeamService;
    @Autowired
    private EventTeamRequestService eventTeamRequestService;
    @Autowired
    private MatchService matchService;
    @Autowired
    private EventMatchService eventMatchService;
    @Autowired
    private EventRefereeService eventRefereeService;
    @Autowired
    private EventRefereeRequestService eventRefereeRequestService;
    @Autowired
    private EventGroupService eventGroupService;
    @Autowired
    private EventGroupTeamService eventGroupTeamService;
    @Autowired
    private TeamPlayerService teamPlayerService;
    @Autowired
    private MatchPlayerService matchPlayerService;
    @Autowired
    private EventStageService eventStageService;
    @Autowired
    private EventStageTagService eventStageTagService;
    @Autowired
    private MatchRefereeService matchRefereeService;
    @Autowired
    private EventCreatorService eventCreatorService;
    @Autowired
    private EventTeamRosterService eventTeamRosterService;

    @Override
    @Transactional
    public boolean createEvent(Event event) {
        if (!save(event)) {
            throw new InternalServerErrorException("创建赛事失败");
        }
        if (!eventStageService.updateStageAndTage(event)) {
            throw new InternalServerErrorException("创建赛事失败");
        }
        if (event.getStageList().stream().map(Event.Stage::getStageName).toList().contains(EventStage.STAGE_CUP_GROUP)) {
            for (Event.Stage stage : event.getStageList()) {
                if (stage.getStageName().equals(EventStage.STAGE_CUP_GROUP)) {
                    for (Event.Tag tag : stage.getTags()) {
                        EventGroup eventGroup = new EventGroup();
                        eventGroup.setEventId(event.getEventId());
                        eventGroup.setName(tag.getTagName());
                        if (!eventGroupService.save(eventGroup)) {
                            throw new InternalServerErrorException("创建杯赛小组失败");
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean updateEvent(Event event) {
        boolean updateMain = updateById(event);
//        boolean updateStage = eventStageService.updateStageAndTage(event);
        return updateMain;
    }

    @Override
    @Transactional
    public boolean deleteEvent(Long eventId) {
        // 1. 删除赛事创建者记录
        QueryWrapper<EventCreator> creatorQueryWrapper = new QueryWrapper<>();
        creatorQueryWrapper.eq("event_id", eventId);
        eventCreatorService.remove(creatorQueryWrapper);

        // 2. 删除赛事球队大名单记录（必须在删除 event_team 之前删除）
        QueryWrapper<EventTeamRoster> rosterQueryWrapper = new QueryWrapper<>();
        rosterQueryWrapper.eq("event_id", eventId);
        eventTeamRosterService.remove(rosterQueryWrapper);

        // 3. 删除赛事本身（级联删除其他相关表）
        if (!removeById(eventId)) {
            throw new RuntimeException("删除赛事失败");
        }
        return true;
    }


    @Override
    public Event getDetailedEvent(Long eventId) {
        Event event = getById(eventId);
        if (event == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        List<EventManager> eventManagers = eventManagerService.list(new QueryWrapper<EventManager>().eq("event_id", eventId));
        event.setManagerIdList(eventManagers.stream().map(EventManager::getUserId).toList());
        List<EventTeam> eventTeams = eventTeamService.listWithTeam(eventId);
        for (EventTeam et : eventTeams) {
            List<TeamPlayer> teamPlayerList = teamPlayerService.listWithPlayer(et.getTeam().getTeamId());
            et.getTeam().setPlayerList(teamPlayerList.stream().map(TeamPlayer::getPlayer).toList());
        }
        event.setTeamList(eventTeams.stream().map(EventTeam::getTeam).map(t -> new Event.Team(t.getTeamId(), t.getName(), t.getLogoUrl(), t.getPlayerList().size())).toList());
        List<EventGroup> eventGroups = eventGroupService.list(new QueryWrapper<EventGroup>().eq("event_id", eventId));
        event.setGroupList(eventGroups.stream().map(g -> {
            Event.Group group = new Event.Group();
            group.setGroupId(g.getGroupId());
            group.setName(g.getName());
            List<EventGroupTeam> eventGroupTeams = eventGroupTeamService.list(new QueryWrapper<EventGroupTeam>().eq("group_id", g.getGroupId()));
            group.setTeamList(eventGroupTeams.stream().map(t -> {
                Event.Group.Team team = new Event.Group.Team();
                for (EventTeam et : eventTeams) {
                    if (et.getTeamId().equals(t.getTeamId())) {
                        team.setTeam(new Event.Team(et.getTeamId(), et.getTeam().getName(), et.getTeam().getLogoUrl(), et.getTeam().getPlayerList().size()));
                        break;
                    }
                }
                team.setNumWins(t.getNumWins());
                team.setNumDraws(t.getNumDraws());
                team.setNumLosses(t.getNumLosses());
                team.setNumGoalsFor(t.getNumGoalsFor());
                team.setNumGoalsAgainst(t.getNumGoalsAgainst());
                team.setScore(t.getScore());
                return team;
            }).toList());
            return group;
        }).toList());
        List<EventMatch> eventMatches = eventMatchService.list(new QueryWrapper<EventMatch>().eq("event_id", eventId));
        eventMatches.forEach(em -> {
            em.setEvent(event);
            Match match = matchService.getById(em.getMatchId());
            for (EventTeam et : eventTeams) {
                if (et.getTeamId().equals(match.getHomeTeamId())) {
                    match.setHomeTeam(et.getTeam());
                }
                if (et.getTeamId().equals(match.getAwayTeamId())) {
                    match.setAwayTeam(et.getTeam());
                }
            }
            match.setMatchEvent(new MatchEvent(em));
            em.setMatch(match);
        });
        event.setMatchList(eventMatches.stream().map(EventMatch::getMatch).sorted(Comparator.comparing(Match::getTime)).toList());
        event.setStageList(eventStageService.list(new QueryWrapper<EventStage>().eq("event_id", eventId)).stream().map(EventStage::getStage).map(stage -> {
            Event.Stage eventStage = new Event.Stage();
            eventStage.setStageName(stage);
            eventStage.setTags(eventStageTagService.list(new QueryWrapper<EventStageTag>().eq("event_id", eventId).eq("stage", stage)).stream().map(EventStageTag::getTag).distinct().map(tag -> {
                Event.Tag eventTag = new Event.Tag();
                eventTag.setTagName(tag);
                eventTag.setMatches(eventMatches.stream().filter(em -> em.getStage().equals(stage) && em.getTag().equals(tag)).map(EventMatch::getMatch).toList());
                return eventTag;
            }).toList());
            return eventStage;
        }).toList());

//        event.setStageList(eventMatches.stream().map(EventMatch::getStage).distinct().map(stage -> {
//            Event.Stage eventStage = new Event.Stage();
//            eventStage.setStageName(stage);
//            eventStage.setTags(eventMatches.stream().filter(em -> em.getStage().equals(stage)).map(EventMatch::getTag).distinct().map(tag -> {
//                Event.Tag eventTag = new Event.Tag();
//                eventTag.setTagName(tag);
//                eventTag.setMatches(eventMatches.stream().filter(em2 -> em2.getStage().equals(stage) && em2.getTag().equals(tag)).map(EventMatch::getMatch).toList());
//                return eventTag;
//            }).toList());
//            return eventStage;
//        }).toList());
        return event;
    }

    @Override
    public boolean inviteManager(EventManager eventManager) {
        if (eventManagerService.selectByMultiId(eventManager) != null) {
            throw new ConflictException("该用户已经是该赛事的管理员");
        }
        if (!eventManagerService.saveOrUpdateByMultiId(eventManager)) {
            throw new RuntimeException("邀请失败");
        }
        return true;
    }

    @Override
    public List<Long> getManagers(Long eventId) {
        QueryWrapper<EventManager> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("event_id", eventId);
        return eventManagerService.list(queryWrapper).stream().map(EventManager::getUserId).toList();
    }

    @Override
    public boolean deleteManager(EventManager eventManager) {
        if (!eventManagerService.deleteByMultiId(eventManager)) {
            throw new RuntimeException("删除管理员失败");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean inviteTeam(EventTeamRequest eventTeamRequest) {
        EventTeam eventTeam = new EventTeam(eventTeamRequest.getEventId(), eventTeamRequest.getTeamId());
        if (eventTeamService.selectByMultiId(eventTeam) != null) {
            throw new ConflictException("该队伍已经是该赛事的参赛队伍");
        }
        EventTeamRequest requestRes = eventTeamRequestService.selectByMultiId(eventTeamRequest);
        if (requestRes != null && requestRes.getStatus().equals(EventTeamRequest.STATUS_PENDING)) {
            throw new ConflictException("该队伍已经被邀请参加赛事");
        }
        if (!eventTeamRequestService.saveOrUpdateByMultiId(eventTeamRequest)) {
            throw new RuntimeException("邀请失败");
        }
        return true;
    }

    @Override
    public List<EventTeamRequest> getTeamInvitations(Long eventId) {
        return eventTeamRequestService.listWithTeam(eventId, EventTeamRequest.TYPE_INVITATION);
    }

    @Override
    public List<EventTeamRequest> getTeamApplications(Long eventId) {
        return eventTeamRequestService.listWithTeam(eventId, EventTeamRequest.TYPE_APPLICATION);
    }

    @Override
    @Transactional
    public boolean replyTeamApplication(EventTeamRequest eventTeamRequest) {
        EventTeam eventTeam = new EventTeam(eventTeamRequest.getEventId(), eventTeamRequest.getTeamId());
        if (eventTeamService.selectByMultiId(eventTeam) != null) {
            throw new ConflictException("该队伍已经是该赛事的参赛队伍");
        }
        if (eventTeamRequestService.selectByMultiId(eventTeamRequest) == null) {
            throw new ResourceNotFoundException("该队伍没有申请参加赛事");
        }
        if (!eventTeamRequestService.updateByMultiId(eventTeamRequest)) {
            throw new RuntimeException("回复球队申请失败");
        }
        if (eventTeamRequest.getStatus().equals(EventTeamRequest.STATUS_ACCEPTED)) {
            if (!eventTeamService.saveOrUpdateByMultiId(eventTeam)) {
                throw new RuntimeException("添加参赛队伍失败");
            }
        }
        return true;
    }

    @Override
    public List<Team> getTeams(Long eventId) {
        return eventTeamService.listWithTeam(eventId).stream().map(EventTeam::getTeam).toList();
    }

    @Override
    public boolean deleteTeam(EventTeam eventTeam) {
        if (eventTeamService.selectByMultiId(eventTeam) == null) {
            throw new ResourceNotFoundException("该队伍不是该赛事的参赛队伍");
        }
        if (!eventTeamService.deleteByMultiId(eventTeam)) {
            throw new RuntimeException("删除参赛队伍失败");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean addMatch(Long eventId, Match match, String stage, String tag) {
        // 添加比赛
        if (!matchService.save(match)) {
            throw new RuntimeException("添加比赛失败");
        }

        // 这一部分应该在比赛开始前添加，此处删除
        // 添加主队比赛球员
//        List<TeamPlayer> homeTeamPlayers = teamPlayerService.list(new QueryWrapper<TeamPlayer>().eq("team_id", match.getHomeTeamId()));
//        List<MatchPlayer> homeTeamMatchPlayers = new ArrayList<>();
//        for (TeamPlayer tp : homeTeamPlayers) {
//            MatchPlayer matchPlayer = new MatchPlayer();
//            matchPlayer.setMatchId(match.getMatchId());
//            matchPlayer.setTeamId(match.getHomeTeamId());
//            matchPlayer.setPlayerId(tp.getPlayerId());
//            matchPlayer.setNumber(tp.getNumber());
//            matchPlayer.setIsStart(false);
//            homeTeamMatchPlayers.add(matchPlayer);
//        }
//        if (!matchPlayerService.saveOrUpdateBatchByMultiId(homeTeamMatchPlayers)) {
//            throw new RuntimeException("添加主队比赛球员失败");
//        }

        // 添加客队比赛球员
//        List<TeamPlayer> awayTeamPlayers = teamPlayerService.list(new QueryWrapper<TeamPlayer>().eq("team_id", match.getAwayTeamId()));
//        List<MatchPlayer> awayTeamMatchPlayers = new ArrayList<>();
//        for (TeamPlayer tp : awayTeamPlayers) {
//            MatchPlayer matchPlayer = new MatchPlayer();
//            matchPlayer.setMatchId(match.getMatchId());
//            matchPlayer.setTeamId(match.getAwayTeamId());
//            matchPlayer.setPlayerId(tp.getPlayerId());
//            matchPlayer.setNumber(tp.getNumber());
//            matchPlayer.setIsStart(false);
//            awayTeamMatchPlayers.add(matchPlayer);
//        }
//        if (!matchPlayerService.saveOrUpdateBatchByMultiId(awayTeamMatchPlayers)) {
//            throw new RuntimeException("添加客队比赛球员失败");
//        }

        // 添加赛事比赛
        EventMatch eventMatch = new EventMatch();
        eventMatch.setEventId(eventId);
        eventMatch.setMatchId(match.getMatchId());
        eventMatch.setStage(stage);
        eventMatch.setTag(tag);
        if (!eventMatchService.saveOrUpdateByMultiId(eventMatch)) {
            throw new RuntimeException("添加赛事比赛失败");
        }
        return true;
    }

    @Override
    public List<Match> getMatches(Long eventId) {
        return eventMatchService.listWithMatch(eventId).stream().map(EventMatch::getMatch).toList();
    }

    @Override
    public boolean deleteMatch(EventMatch eventMatch) {
        if (eventMatchService.selectByMultiId(eventMatch) == null) {
            throw new ResourceNotFoundException("赛事不含该比赛");
        }
        if (!eventMatchService.deleteByMultiId(eventMatch)) {
            throw new RuntimeException("删除赛事比赛失败");
        }
        return true;
    }

    @Override
    public boolean inviteReferee(EventRefereeRequest eventRefereeRequest) {
        EventReferee eventReferee = new EventReferee(eventRefereeRequest.getEventId(), eventRefereeRequest.getRefereeId());
        if (eventRefereeService.selectByMultiId(eventReferee) != null) {
            throw new ConflictException("该裁判已经执法该赛事");
        }
        EventRefereeRequest eventRefereeRes = eventRefereeRequestService.selectByMultiId(eventRefereeRequest);
        if (eventRefereeRes != null && eventRefereeRes.getStatus().equals(EventRefereeRequest.STATUS_PENDING)) {
            throw new ConflictException("该裁判正在被邀请执法该赛事");
        }
        if (!eventRefereeRequestService.saveOrUpdateByMultiId(eventRefereeRequest)) {
            throw new RuntimeException("邀请失败");
        }
        return true;
    }

    @Override
    public List<Referee> getReferees(Long eventId) {
        return eventRefereeService.listWithReferee(eventId).stream().map(EventReferee::getReferee).toList();
    }

    @Override
    public boolean deleteReferee(EventReferee eventReferee) {
        if (eventRefereeService.selectByMultiId(eventReferee) == null) {
            throw new ResourceNotFoundException("该裁判不是该赛事的裁判");
        }
        if (!eventRefereeService.deleteByMultiId(eventReferee)) {
            throw new RuntimeException("删除赛事裁判失败");
        }
        return true;
    }

    @Override
    public boolean setMatchReferee(Long eventId, Long matchId, Long refereeId) {
        EventMatch eventMatch = new EventMatch(eventId, matchId);
        if (eventMatchService.selectByMultiId(eventMatch) == null) {
            throw new ResourceNotFoundException("赛事不含该比赛");
        }
        EventReferee eventReferee = new EventReferee(eventId, refereeId);
        if (eventRefereeService.selectByMultiId(eventReferee) == null) {
            throw new ResourceNotFoundException("赛事不含该裁判");
        }
        MatchReferee matchReferee = new MatchReferee();
        matchReferee.setMatchId(matchId);
        matchReferee.setRefereeId(refereeId);
        if (!matchRefereeService.saveOrUpdateByMultiId(matchReferee)) {
            throw new RuntimeException("设置比赛裁判失败");
        }
        return true;
    }
}