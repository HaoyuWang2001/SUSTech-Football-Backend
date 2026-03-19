package com.sustech.football.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sustech.football.entity.*;
import com.sustech.football.exception.BadRequestException;
import com.sustech.football.exception.InternalServerErrorException;
import com.sustech.football.exception.ResourceNotFoundException;
import com.sustech.football.model.match.*;
import com.sustech.football.service.*;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/match")
@Tag(name = "Match Controller", description = "管理比赛的接口")
public class MatchController {
    @Autowired
    private MatchService matchService;
    @Autowired
    private UserService userService;
    @Autowired
    private TeamService teamService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private MatchLiveService matchLiveService;
    @Autowired
    private MatchVideoService matchVideoService;
    @Autowired
    private MatchPlayerService matchPlayerService;
    @Autowired
    private RefereeService refereeService;
    @Autowired
    private SecondLevelAuthorityService secondLevelAuthorityService;
    @Autowired
    private ThirdLevelAuthorityService thirdLevelAuthorityService;
    @Autowired
    private MatchCreatorService matchCreatorService;
    @Autowired
    private TeamCreatorService teamCreatorService;
    @Autowired
    private EventTeamRosterService eventTeamRosterService;

    @PostMapping("/create")
    @Transactional
    @Operation(summary = "创建比赛", description = "创建一个新的比赛")
    @Parameters({
            @Parameter(name = "ownerId", description = "比赛所有者 ID", required = true),
            @Parameter(name = "createAuthorityLevel", description = "创建者的权限等级", required = true),
            @Parameter(name = "createAuthorityId", description = "创建者的权限 ID", required = true)
    })
    public Long createMatch(Long ownerId, Integer createAuthorityLevel, Long createAuthorityId, @RequestBody Match match) {
        /*
          该方法仅用来创建“友谊赛”，不用来创建赛事比赛（赛事比赛通过：/event/match/add 创建）
          创建比赛（友谊赛）时，match仅给定比赛时间、主队ID，之后可能增加地点，其余要素均不给。
          不要将主队球员注入MatchPlayer中，这一步完全由裁判进行操作。
         */
        if (ownerId == null) {
            throw new BadRequestException("比赛所有者ID不能为空");
        }
        if (userService.getById(ownerId) == null) {
            throw new BadRequestException("比赛所有者不存在");
        }
        if (match == null) {
            throw new BadRequestException("比赛信息不能为空");
        }
        if (match.getMatchId() != null) {
            throw new BadRequestException("新建的比赛, 其ID不能存在");
        }
        if (!matchService.save(match)) {
            throw new BadRequestException("创建比赛失败");
        }
        if (!matchService.inviteManager(new MatchManager(match.getMatchId(), ownerId, true))) {
            throw new BadRequestException("比赛管理员设置失败");
        }

        MatchCreator matchCreator = new MatchCreator();
        matchCreator.setMatchId(match.getMatchId());
        matchCreator.setUserId(ownerId);
        matchCreator.setCreateAuthorityLevel(createAuthorityLevel);
        matchCreator.setCreateAuthorityId(createAuthorityId);
        if (!matchCreatorService.save(matchCreator)) {
            throw new InternalServerErrorException("创建比赛失败");
        }

        return match.getMatchId();
    }

    @GetMapping("/get")
    @Operation(summary = "获取比赛信息", description = "根据比赛ID获取比赛信息")
    @Parameter(name = "id", description = "比赛 ID", required = true)
    public VoMatch getMatch(Long id) {
        if (id == null) {
            throw new BadRequestException("比赛ID不能为空");
        }
        VoMatch voMatch = new VoMatch();
        voMatch.setMatchId(id);

        // 比赛基本信息
        Match match = matchService.getById(id);
        if (match == null) {
            throw new BadRequestException("比赛不存在");
        }
        voMatch.setTime(match.getTime());
        voMatch.setStatus(match.getStatus());

        // 主队
        Team homeTeam = teamService.getById(match.getHomeTeamId());
        if (homeTeam == null) {
            homeTeam = new Team();
        }
        List<MatchPlayer> homePlayers = matchPlayerService.list(new QueryWrapper<MatchPlayer>().eq("match_id", id).eq("team_id", match.getHomeTeamId()));
        List<VoMatchPlayer> voHomePlayers = new ArrayList<>();
        for (MatchPlayer matchPlayer : homePlayers) {
            Player player = playerService.getById(matchPlayer.getPlayerId());
            VoMatchPlayer voMatchPlayer = new VoMatchPlayer();
            voMatchPlayer.setPlayerId(player.getPlayerId());
            voMatchPlayer.setNumber(matchPlayer.getNumber());
            voMatchPlayer.setName(player.getName());
            voMatchPlayer.setPhotoUrl(player.getPhotoUrl());
            voMatchPlayer.setIsStart(matchPlayer.getIsStart());
            voHomePlayers.add(voMatchPlayer);
        }
        VoMatchTeam voHomeTeam = new VoMatchTeam();
        voHomeTeam.setTeamId(homeTeam.getTeamId());
        voHomeTeam.setName(homeTeam.getName());
        voHomeTeam.setLogoUrl(homeTeam.getLogoUrl());
        voHomeTeam.setScore(match.getHomeTeamScore());
        voHomeTeam.setPenalty(match.getHomeTeamPenalty());
        voHomeTeam.setPlayers(voHomePlayers);
        voMatch.setHomeTeam(voHomeTeam);

        // 客队
        Team awayTeam = teamService.getById(match.getAwayTeamId());
        if (awayTeam == null) {
            awayTeam = new Team();
        }
        List<MatchPlayer> awayPlayers = matchPlayerService.list(new QueryWrapper<MatchPlayer>().eq("match_id", id).eq("team_id", match.getAwayTeamId()));
        List<VoMatchPlayer> voAwayPlayers = new ArrayList<>();
        for (MatchPlayer matchPlayer : awayPlayers) {
            Player player = playerService.getById(matchPlayer.getPlayerId());
            VoMatchPlayer voMatchPlayer = new VoMatchPlayer();
            voMatchPlayer.setPlayerId(player.getPlayerId());
            voMatchPlayer.setNumber(matchPlayer.getNumber());
            voMatchPlayer.setName(player.getName());
            voMatchPlayer.setPhotoUrl(player.getPhotoUrl());
            voMatchPlayer.setIsStart(matchPlayer.getIsStart());
            voAwayPlayers.add(voMatchPlayer);
        }
        VoMatchTeam voAwayTeam = new VoMatchTeam();
        voAwayTeam.setTeamId(awayTeam.getTeamId());
        voAwayTeam.setName(awayTeam.getName());
        voAwayTeam.setLogoUrl(awayTeam.getLogoUrl());
        voAwayTeam.setScore(match.getAwayTeamScore());
        voAwayTeam.setPenalty(match.getAwayTeamPenalty());
        voAwayTeam.setPlayers(voAwayPlayers);
        voMatch.setAwayTeam(voAwayTeam);

        // 管理员
        List<Long> managerList = matchService.getManagers(id);
        voMatch.setManagerList(managerList);

        // 裁判
        List<Referee> refereeList = matchService.getReferees(id);
        List<VoMatchReferee> voRefereeList = new ArrayList<>();
        for (Referee referee : refereeList) {
            VoMatchReferee voMatchReferee = new VoMatchReferee();
            voMatchReferee.setRefereeId(referee.getRefereeId());
            voMatchReferee.setName(referee.getName());
            voMatchReferee.setPhotoUrl(referee.getPhotoUrl());
            voRefereeList.add(voMatchReferee);
        }
        voMatch.setRefereeList(voRefereeList);

        // 比赛事件
        List<MatchPlayerAction> matchPlayerActions = matchService.getMatchPlayerActions(id);
        List<VoMatchPlayerAction> voMatchPlayerActions = new ArrayList<>();
        for (MatchPlayerAction matchPlayerAction : matchPlayerActions) {
            VoMatchPlayerAction voMatchPlayerAction = new VoMatchPlayerAction();
            voMatchPlayerAction.setTeamId(matchPlayerAction.getTeamId());
            VoMatchPlayer voMatchPlayer;
            if (matchPlayerAction.getTeamId().equals(match.getHomeTeamId())) {
                voMatchPlayer = voHomeTeam.getPlayers()
                        .stream()
                        .filter(voMatchPlayer1 -> voMatchPlayer1.getPlayerId().equals(matchPlayerAction.getPlayerId()))
                        .findFirst()
                        .orElse(null);
            } else {
                voMatchPlayer = voAwayTeam.getPlayers()
                        .stream()
                        .filter(voMatchPlayer1 -> voMatchPlayer1.getPlayerId().equals(matchPlayerAction.getPlayerId()))
                        .findFirst()
                        .orElse(null);
            }
            if (voMatchPlayer == null) {
                throw new InternalServerErrorException("比赛球员动作信息错误，未找到对应球员");
            }
            voMatchPlayerAction.setPlayer(voMatchPlayer);
            voMatchPlayerAction.setTime(matchPlayerAction.getTime());
            voMatchPlayerAction.setAction(matchPlayerAction.getAction());
            voMatchPlayerActions.add(voMatchPlayerAction);
        }
        voMatchPlayerActions.sort(Comparator.comparing(VoMatchPlayerAction::getTime));
        voMatch.setMatchPlayerActionList(voMatchPlayerActions);

        // 所属赛事
        MatchEvent matchEvent = matchService.findMatchEvent(match);
        VoMatchEvent voMatchEvent = new VoMatchEvent();
        voMatchEvent.setEventId(matchEvent.getEventId());
        voMatchEvent.setEventName(matchEvent.getEventName());
        voMatchEvent.setStage(matchEvent.getMatchStage());
        voMatchEvent.setTag(matchEvent.getMatchTag());
        voMatch.setMatchEvent(voMatchEvent);

        return voMatch;
    }

    @GetMapping("/getAll")
    @Operation(summary = "获取所有比赛", description = "获取所有比赛信息")
    public List<Match> getAllMatches() {
        List<Match> matchList = matchService.list();
        matchList.forEach(match -> {
            match.setMatchEvent(matchService.findMatchEvent(match));
            match.setHomeTeam(teamService.getById(match.getHomeTeamId()));
            match.setAwayTeam(teamService.getById(match.getAwayTeamId()));
        });
        return matchList.stream().sorted(Comparator.comparing(Match::getTime).reversed()).toList();
    }

    @GetMapping("/getAllFriendlyMatches")
    @Operation(summary = "获取所有友谊赛", description = "获取所有友谊赛信息")
    public List<Match> getAllFriendlyMatches() {
        List<Match> matchList = matchService.list();
        matchList.forEach(match -> {
            match.setMatchEvent(matchService.findMatchEvent(match));
            match.setHomeTeam(teamService.getById(match.getHomeTeamId()));
            match.setAwayTeam(teamService.getById(match.getAwayTeamId()));
        });
        return matchList.stream()
                .filter(match -> match.getMatchEvent() == null || match.getMatchEvent().getEventId() == null)
                .sorted(Comparator.comparing(Match::getTime).reversed()).toList();
    }

    @GetMapping("/getByIdList")
    @Operation(summary = "获取比赛信息", description = "根据比赛ID列表获取比赛信息")
    @Parameter(name = "idList", description = "比赛 ID 列表", required = true)
    public List<Match> getMatchByIdList(@RequestParam List<Long> idList) {
        if (idList == null) {
            throw new BadRequestException("比赛ID列表不能为空");
        }
        return matchService.getMatchByIdList(idList);
    }

    @GetMapping("/getFriendlyMatchesBySecondAuthority")
    @Operation(summary = "获取比赛信息", description = "根据二级权限ID获取友谊赛信息")
    @Parameter(name = "authorityId", description = "二级权限 ID", required = true)
    public List<Match> getFriendlyMatchesBySecondAuthority(Long authorityId) {
        if (authorityId == null) {
            throw new BadRequestException("二级权限ID不能为空");
        }

        if (secondLevelAuthorityService.getById(authorityId) == null) {
            throw new ResourceNotFoundException("二级权限不存在");
        }

        /**
         * 二级权限下的友谊赛：先获取二级权限+三级权限**创建**的球队，再获取这些球队的友谊赛
         */

        // 获取二级权限下的所有三级权限
        QueryWrapper<ThirdLevelAuthority> thirdLevelAuthorityQueryWrapper = new QueryWrapper<>();
        thirdLevelAuthorityQueryWrapper.eq("second_level_authority_id", authorityId);
        List<ThirdLevelAuthority> thirdLevelAuthorityList = thirdLevelAuthorityService.list(thirdLevelAuthorityQueryWrapper);
        List<Long> thirdAuthorityAuthorityIdList = thirdLevelAuthorityList.stream().map(ThirdLevelAuthority::getAuthorityId).toList();

        // 获取二级权限创建的所有球队
        QueryWrapper<TeamCreator> teamCreatorQueryWrapper = new QueryWrapper<>();
        teamCreatorQueryWrapper.eq("create_authority_level", 2);
        teamCreatorQueryWrapper.eq("create_authority_id", authorityId);
        List<TeamCreator> teamCreatorList = teamCreatorService.list(teamCreatorQueryWrapper);

        // 获取三级权限创建的所有球队
        if (!thirdAuthorityAuthorityIdList.isEmpty()) {
            teamCreatorQueryWrapper = new QueryWrapper<>();
            teamCreatorQueryWrapper.eq("create_authority_level", 3);
            teamCreatorQueryWrapper.in("create_authority_id", thirdAuthorityAuthorityIdList);
            teamCreatorList.addAll(teamCreatorService.list(teamCreatorQueryWrapper));
        }

        List<Long> teamIdList = teamCreatorList.stream().map(TeamCreator::getTeamId).sorted().toList();
        List<Match> matchList = new ArrayList<>();
        for (Long teamId : teamIdList) {
            matchList.addAll(teamService.getFriendlyMatches(teamId));
        }

        return matchList;

        /**
         * 下面的实现为：获取二级权限级它的三级权限创建的友谊赛，改方式弃用
         */

//        // 获取二级权限下的所有三级权限
//        QueryWrapper<ThirdLevelAuthority> thirdLevelAuthorityQueryWrapper = new QueryWrapper<>();
//        thirdLevelAuthorityQueryWrapper.eq("second_level_authority_id", authorityId);
//        List<ThirdLevelAuthority> thirdLevelAuthorityList = thirdLevelAuthorityService.list(thirdLevelAuthorityQueryWrapper);
//        List<Long> thirdAuthorityAuthorityIdList = thirdLevelAuthorityList.stream().map(ThirdLevelAuthority::getAuthorityId).toList();
//
//        // 获取二级权限下的所有比赛
//        QueryWrapper<MatchCreator> matchCreatorQueryWrapper = new QueryWrapper<>();
//        matchCreatorQueryWrapper.in("create_authority_level", 2);
//        matchCreatorQueryWrapper.eq("create_authority_id", authorityId);
//        List<MatchCreator> matchCreatorList = matchCreatorService.list(matchCreatorQueryWrapper);
//
//        // 获取三级权限下的所有比赛
//        if (!thirdAuthorityAuthorityIdList.isEmpty()) {
//            matchCreatorQueryWrapper = new QueryWrapper<>();
//            matchCreatorQueryWrapper.in("create_authority_level", 3);
//            matchCreatorQueryWrapper.in("create_authority_id", thirdAuthorityAuthorityIdList);
//            matchCreatorList.addAll(matchCreatorService.list(matchCreatorQueryWrapper));
//        }
    }

    @PutMapping("/update")
    @Operation(summary = "更新比赛信息", description = "只有比赛管理员才能更新比赛信息，更新时，request body 必须包含所有想要更改的属性和不想更改的属性！")
    public void updateMatch(@RequestBody Match match) {
        if (match == null) {
            throw new BadRequestException("传参含空值");
        }
        if (match.getMatchId() == null) {
            throw new BadRequestException("比赛ID不能为空");
        }
        if (matchService.getById(match.getMatchId()) == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        if (!matchService.updateById(match)) {
            throw new InternalServerErrorException("更新失败");
        }

    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除比赛", description = "只有比赛管理员才能删除比赛")
    @Parameters({
            @Parameter(name = "matchId", description = "比赛 ID", required = true),
            @Parameter(name = "userId", description = "用户 ID", required = true)
    })
    public void deleteMatch(Long matchId, Long userId) {
        if (matchId == null || userId == null) {
            throw new BadRequestException("传参含空");
        }
        if (matchService.getById(matchId) == null) {
            throw new BadRequestException("比赛不存在");
        }
        if (userId != 0 && userService.getById(userId) == null) {
            throw new BadRequestException("用户不存在");
        }
        if (!matchService.deleteMatch(matchId, userId)) {
            throw new BadRequestException("删除比赛失败");
        }
    }

    @PostMapping("/manager/invite")
    @Operation(summary = "邀请管理员", description = "提供比赛 ID 和管理员ID，邀请管理员")
    @Parameters({
            @Parameter(name = "matchId", description = "比赛 ID", required = true),
            @Parameter(name = "managerId", description = "管理员 ID", required = true)
    })
    public void inviteManager(Long matchId, Long managerId) {
        if (matchId == null || managerId == null) {
            throw new BadRequestException("比赛ID和管理员ID不能为空");
        }
        if (matchService.getById(matchId) == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        if (userService.getById(managerId) == null) {
            throw new ResourceNotFoundException("管理员不存在");
        }
        if (matchService.inviteManager(new MatchManager(matchId, managerId, false))) {
            throw new RuntimeException("邀请管理员失败");
        }
    }

    @GetMapping("/manager/getAll")
    @Operation(summary = "获取所有管理员", description = "提供比赛 ID，获取比赛的所有管理员")
    @Parameter(name = "matchId", description = "比赛 ID", required = true)
    public List<Long> getManagers(Long matchId) {
        if (matchId == null) {
            throw new BadRequestException("比赛ID不能为空");
        }
        if (matchService.getById(matchId) == null) {
            throw new BadRequestException("比赛不存在");
        }
        return matchService.getManagers(matchId);
    }

    @DeleteMapping("/manager/delete")
    @Operation(summary = "删除管理员", description = "提供比赛 ID 和管理员ID，删除管理员")
    @Parameters({
            @Parameter(name = "matchId", description = "比赛 ID", required = true),
            @Parameter(name = "managerId", description = "管理员 ID", required = true)
    })
    public void deleteManager(Long matchId, Long managerId) {
        if (matchId == null || managerId == null) {
            throw new BadRequestException("比赛ID和管理员ID不能为空");
        }
        if (matchService.getById(matchId) == null) {
            throw new BadRequestException("比赛不存在");
        }
        if (userService.getById(managerId) == null) {
            throw new BadRequestException("管理员不存在");
        }
        if (!matchService.deleteManager(new MatchManager(matchId, managerId, false))) {
            throw new BadRequestException("删除管理员失败");
        }
    }

    @PostMapping("/team/invite")
    @Operation(summary = "邀请球队", description = "提供比赛 ID 和球队ID，邀请球队")
    @Parameters({
            @Parameter(name = "matchId", description = "比赛 ID", required = true),
            @Parameter(name = "teamId", description = "球队 ID", required = true),
            @Parameter(name = "isHomeTeam", description = "是否主队", required = true)
    })
    public void inviteTeam(Long matchId, Long teamId, Boolean isHomeTeam) {
        if (matchId == null || teamId == null || isHomeTeam == null) {
            throw new BadRequestException("传参含空值");
        }
        if (matchService.getById(matchId) == null) {
            throw new BadRequestException("比赛不存在");
        }
        if (teamService.getById(teamId) == null) {
            throw new BadRequestException("球队不存在");
        }
        String type = isHomeTeam ? MatchTeamRequest.TYPE_HOME : MatchTeamRequest.TYPE_AWAY;

        MatchTeamRequest matchTeamRequest = new MatchTeamRequest();
        matchTeamRequest.setMatchId(matchId);
        matchTeamRequest.setTeamId(teamId);
        matchTeamRequest.setType(type);
        if (!matchService.inviteTeam(matchTeamRequest)) {
            throw new BadRequestException("邀请失败");
        }
    }

    @GetMapping("/team/getInvitations")
    @Operation(summary = "获取球队邀请", description = "提供比赛 ID，获取比赛的所有球队邀请")
    @Parameter(name = "matchId", description = "比赛 ID", required = true)
    public List<MatchTeamRequest> getTeamInvitations(Long matchId) {
        if (matchId == null) {
            throw new BadRequestException("比赛ID不能为空");
        }
        if (matchService.getById(matchId) == null) {
            throw new BadRequestException("比赛不存在");
        }
        return matchService.getTeamInvitations(matchId);
    }

    @GetMapping("/team/get")
    @Operation(summary = "获取球队", description = "提供比赛 ID 和主客队标识，获取比赛的球队信息")
    @Parameters({
            @Parameter(name = "matchId", description = "比赛 ID", required = true),
            @Parameter(name = "isHomeTeam", description = "是否主队", required = true),
            @Parameter(name = "isEventMatch", description = "是否为赛事比赛", required = true)
    })
    public VoMatchTeam getTeam(Long matchId, Boolean isHomeTeam, Boolean isEventMatch) {
        if (matchId == null || isHomeTeam == null || isEventMatch == null) {
            throw new BadRequestException("比赛ID、主客队标识和是否赛事比赛不能为空");
        }
        Match match = matchService.getById(matchId);
        if (match == null) {
            throw new BadRequestException("比赛不存在");
        }

        Long teamId = isHomeTeam ? match.getHomeTeamId() : match.getAwayTeamId();
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BadRequestException("球队不存在");
        }

        // 最终返回的球员列表
        List<VoMatchPlayer> voPlayers = new ArrayList<>();

        // 目前储存在数据库中，该比赛对应的球员列表
        List<MatchPlayer> matchPlayerList = matchPlayerService.list(new QueryWrapper<MatchPlayer>().eq("match_id", matchId).eq("team_id", teamId));
        for (MatchPlayer matchPlayer : matchPlayerList) { // 填入matchPlayer.player字段
            Player player = playerService.getById(matchPlayer.getPlayerId());
            matchPlayer.setPlayer(player);
        }
        if (Boolean.TRUE.equals(isEventMatch)) {
            MatchEvent matchEvent = matchService.findMatchEvent(match);
            if (matchEvent == null || matchEvent.getEventId() == null) {
                throw new BadRequestException("比赛不属于任何赛事，无法获取赛事大名单");
            }

            List<Player> rosterPlayers = eventTeamRosterService.getRoster(matchEvent.getEventId(), teamId);
            Map<Long, MatchPlayer> matchPlayerMap = matchPlayerList.stream().collect(Collectors.toMap(MatchPlayer::getPlayerId, mp -> mp, (a, b) -> a));
            Map<Long, Integer> teamNumberMap = teamService.getTeamPlayers(teamId)
                    .stream()
                    .collect(Collectors.toMap(TeamPlayer::getPlayerId, TeamPlayer::getNumber, (a, b) -> a));

            List<MatchPlayer> rosterMatchPlayers = new ArrayList<>();
            for (Player rosterPlayer : rosterPlayers) {
                MatchPlayer matchPlayer = matchPlayerMap.get(rosterPlayer.getPlayerId());
                if (matchPlayer == null) {
                    matchPlayer = new MatchPlayer();
                    matchPlayer.setMatchId(matchId);
                    matchPlayer.setTeamId(teamId);
                    matchPlayer.setPlayerId(rosterPlayer.getPlayerId());
                    matchPlayer.setIsStart(false);
                }
                matchPlayer.setPlayer(rosterPlayer);
                if (matchPlayer.getNumber() == null) {
                    matchPlayer.setNumber(teamNumberMap.get(rosterPlayer.getPlayerId()));
                }
                rosterMatchPlayers.add(matchPlayer);
            }
            matchPlayerList = rosterMatchPlayers;
        } else if (match.getStatus().equals(Match.STATUS_PENDING)) { // 当比赛未开始的时候，可以更新比赛对应的球员列表及是否首发
            // 获取球队当前的所有球员
            List<TeamPlayer> teamCurrentPlayerList = teamService.getTeamPlayers(teamId);

            // 球队当前的所有球员均需要加入到比赛的球员列表中，若之前已经加入的则更新信息并且isStart保持不变，若新加入的则填入信息且isStart字段设置为false
            Set<Player> matchPlayerSet = matchPlayerList.stream().map(MatchPlayer::getPlayer).collect(Collectors.toSet());
            for (TeamPlayer teamPlayer : teamCurrentPlayerList) {
                Player player = teamPlayer.getPlayer();
                Integer number = teamPlayer.getNumber();
                if (matchPlayerSet.contains(player)) {
                    MatchPlayer matchPlayer = matchPlayerList.stream().filter(mp -> mp.getPlayerId().equals(player.getPlayerId())).findFirst().orElse(null);
                    if (matchPlayer == null) {
                        throw new InternalServerErrorException("比赛球员信息错误");
                    }
                    matchPlayer.setPlayer(player);
                    matchPlayer.setNumber(number);
                    matchPlayer.setIsStart(matchPlayer.getIsStart()); // isStart保持不变
                } else {
                    MatchPlayer matchPlayer = new MatchPlayer();
                    matchPlayer.setMatchId(matchId);
                    matchPlayer.setTeamId(teamId);
                    matchPlayer.setPlayerId(player.getPlayerId());
                    matchPlayer.setPlayer(player);
                    matchPlayer.setNumber(number);
                    matchPlayer.setIsStart(false);
                    matchPlayerList.add(matchPlayer);
                }
            }

            // 检查是否有球员已经不在球队中，若不在则删除
            Set<Player> teamCurrentPlayerSet = teamCurrentPlayerList.stream().map(TeamPlayer::getPlayer).collect(Collectors.toSet());
            List<MatchPlayer> matchPlayerList_needToDelete = new ArrayList<>();
            for (MatchPlayer matchPlayer : matchPlayerList) {
                Player player = matchPlayer.getPlayer();
                if (!teamCurrentPlayerSet.contains(player)) {
                    matchPlayerList_needToDelete.add(matchPlayer);
                }
            }
            for (MatchPlayer matchPlayer_needToDelete : matchPlayerList_needToDelete) {
                matchPlayerList.remove((matchPlayer_needToDelete));
            }
        }
        for (MatchPlayer matchPlayer : matchPlayerList) {
            Player player = matchPlayer.getPlayer();
            VoMatchPlayer voMatchPlayer = new VoMatchPlayer();
            voMatchPlayer.setPlayerId(player.getPlayerId());
            voMatchPlayer.setNumber(matchPlayer.getNumber());
            voMatchPlayer.setName(player.getName());
            voMatchPlayer.setPhotoUrl(player.getPhotoUrl());
            voMatchPlayer.setIsStart(matchPlayer.getIsStart());
            voPlayers.add(voMatchPlayer);
        }


        VoMatchTeam voTeam = new VoMatchTeam();
        voTeam.setTeamId(team.getTeamId());
        voTeam.setName(team.getName());
        voTeam.setLogoUrl(team.getLogoUrl());
        voTeam.setScore(match.getHomeTeamScore());
        voTeam.setPenalty(match.getHomeTeamPenalty());
        voTeam.setPlayers(voPlayers);

        return voTeam;
    }

    @DeleteMapping("/team/delete")
    @Operation(summary = "删除球队", description = "提供比赛 ID 和主客队标识，删除比赛的球队")
    @Parameters({
            @Parameter(name = "matchId", description = "比赛 ID", required = true),
            @Parameter(name = "isHomeTeam", description = "是否主队", required = true)
    })
    public void deleteTeam(Long matchId, Boolean isHomeTeam) {
        if (matchId == null) {
            throw new BadRequestException("比赛ID不能为空");
        }
        if (matchService.getById(matchId) == null) {
            throw new BadRequestException("比赛不存在");
        }
        String type = isHomeTeam ? MatchTeamRequest.TYPE_HOME : MatchTeamRequest.TYPE_AWAY;
        if (!matchService.deleteTeam(matchId, isHomeTeam)) {
            throw new BadRequestException("删除球队失败");
        }
    }

    @PostMapping("/referee/invite")
    @Operation(summary = "邀请裁判", description = "提供比赛 ID 和裁判 ID，邀请裁判")
    @Parameters({
            @Parameter(name = "matchId", description = "比赛 ID", required = true),
            @Parameter(name = "refereeId", description = "裁判 ID", required = true)
    })
    public void inviteReferee(Long matchId, Long refereeId) {
        if (matchId == null || refereeId == null) {
            throw new BadRequestException("比赛ID和裁判ID不能为空");
        }
        if (matchService.getById(matchId) == null) {
            throw new BadRequestException("比赛不存在");
        }
        if (userService.getById(refereeId) == null) {
            throw new BadRequestException("裁判不存在");
        }
        if (!matchService.inviteReferee(new MatchReferee(matchId, refereeId))) {
            throw new BadRequestException("邀请裁判失败");
        }

    }

    @GetMapping("/referee/getAll")
    @Operation(summary = "获取所有裁判", description = "提供比赛 ID，获取比赛的所有裁判")
    @Parameter(name = "matchId", description = "比赛 ID", required = true)
    public List<Referee> getReferees(Long matchId) {
        if (matchId == null) {
            throw new BadRequestException("比赛ID不能为空");
        }
        if (matchService.getById(matchId) == null) {
            throw new BadRequestException("比赛不存在");
        }
        return matchService.getReferees(matchId);
    }

    @DeleteMapping("/referee/delete")
    @Operation(summary = "删除裁判", description = "提供比赛 ID 和裁判 ID，删除裁判")
    @Parameters({
            @Parameter(name = "matchId", description = "比赛 ID", required = true),
            @Parameter(name = "refereeId", description = "裁判 ID", required = true)
    })
    public void deleteReferee(Long matchId, Long refereeId) {
        if (matchId == null || refereeId == null) {
            throw new BadRequestException("比赛ID和裁判ID不能为空");
        }
        if (matchService.getById(matchId) == null) {
            throw new BadRequestException("比赛不存在");
        }
        if (userService.getById(refereeId) == null) {
            throw new BadRequestException("裁判不存在");
        }
        if (!matchService.deleteReferee(new MatchReferee(matchId, refereeId))) {
            throw new BadRequestException("删除裁判失败");
        }
    }

    @GetMapping("/referee/getPlayerList")
    @Operation(summary = "获取比赛球员列表", description = "提供裁判 ID、比赛 ID 和球队 ID，获取比赛的球员列表")
    @Parameters({
            @Parameter(name = "refereeId", description = "裁判 ID", required = true),
            @Parameter(name = "matchId", description = "比赛 ID", required = true),
            @Parameter(name = "teamId", description = "球队 ID", required = true)
    })
    public List<VoMatchPlayer> getPlayerList(Long refereeId, Long matchId, Long teamId) {
        if (refereeId == null || matchId == null || teamId == null) {
            throw new BadRequestException("裁判ID和比赛ID和球队ID不能为空");
        }
        Match match = matchService.getById(matchId);
        if (match == null) {
            throw new BadRequestException("比赛不存在");
        }
        if (teamService.getById(teamId) == null) {
            throw new BadRequestException("球队不存在");
        }
        // 球队存在于比赛中
        if (!match.getHomeTeamId().equals(teamId) && !match.getAwayTeamId().equals(teamId)) {
            throw new BadRequestException("球队不在比赛中");
        }
        // 裁判是比赛的裁判
        Referee referee = refereeService.getById(refereeId);
        if (referee == null) {
            throw new BadRequestException("裁判不存在");
        }
        List<Referee> refereeList = matchService.getReferees(matchId);
        if (!refereeList.contains(referee)) {
            throw new BadRequestException("裁判不是比赛的裁判");
        }

        // 获取球员列表
        List<MatchPlayer> homePlayers = matchPlayerService.list(new QueryWrapper<MatchPlayer>().eq("match_id", matchId).eq("team_id", match.getHomeTeamId()));
        List<VoMatchPlayer> voMatchPlayerList = new ArrayList<>();
        for (MatchPlayer matchPlayer : homePlayers) {
            Player player = playerService.getById(matchPlayer.getPlayerId());
            VoMatchPlayer voMatchPlayer = new VoMatchPlayer();
            voMatchPlayer.setPlayerId(player.getPlayerId());
            voMatchPlayer.setNumber(matchPlayer.getNumber());
            voMatchPlayer.setName(player.getName());
            voMatchPlayer.setPhotoUrl(player.getPhotoUrl());
            voMatchPlayer.setIsStart(matchPlayer.getIsStart());
            voMatchPlayerList.add(voMatchPlayer);
        }
        return voMatchPlayerList;
    }

    @PostMapping("/referee/setPlayerList")
    @Transactional
    @Operation(summary = "设置比赛球员列表", description = "将删除对应于该比赛、该球队的原有球员列表，然后插入新的球员列表")
    public void setPlayerList(Long refereeId, Long matchId, Long teamId, @RequestBody List<VoMatchPlayer> voMatchPlayerList) {
        if (refereeId == null || matchId == null || teamId == null) {
            throw new BadRequestException("裁判ID和比赛ID和球队ID不能为空");
        }
        Match match = matchService.getById(matchId);
        if (match == null) {
            throw new BadRequestException("比赛不存在");
        }
        if (teamService.getById(teamId) == null) {
            throw new BadRequestException("球队不存在");
        }
        // 球队需存在于比赛中
        if (!match.getHomeTeamId().equals(teamId) && !match.getAwayTeamId().equals(teamId)) {
            throw new BadRequestException("球队不在比赛中");
        }
        // 裁判需是比赛的裁判
        Referee referee = refereeService.getById(refereeId);
        if (referee == null) {
            throw new BadRequestException("裁判不存在");
        }
        List<Referee> refereeList = matchService.getReferees(matchId);
        if (!refereeList.contains(referee)) {
            throw new BadRequestException("裁判不是比赛的裁判");
        }

        // 维护待插入的球员列表
        List<MatchPlayer> matchPlayerList = new ArrayList<>();
        for (VoMatchPlayer voMatchPlayer : voMatchPlayerList) {
            MatchPlayer matchPlayer = new MatchPlayer();
            matchPlayer.setMatchId(matchId);
            matchPlayer.setTeamId(teamId);
            matchPlayer.setPlayerId(voMatchPlayer.getPlayerId());
            matchPlayer.setNumber(voMatchPlayer.getNumber());
            matchPlayer.setIsStart(voMatchPlayer.getIsStart());
            matchPlayerList.add(matchPlayer);
        }

        // 删除该match该team的原有球员列表
        QueryWrapper<MatchPlayer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId).eq("team_id", teamId);
        matchPlayerService.remove(queryWrapper);

        // 插入新的球员列表
        if (!matchPlayerService.saveOrUpdateBatchByMultiId(matchPlayerList)) {
            throw new InternalServerErrorException("插入新球员列表失败");
        }
    }

    @PostMapping("/referee/updateResult")
    @Operation(summary = "更新比赛结果", description = "提供裁判 ID 和比赛信息，更新比赛结果")
    public void updateResult(Long refereeId, @RequestBody VoMatchInfoForReferee voMatch) {
        if (refereeId == null || voMatch == null) {
            throw new BadRequestException("裁判ID和比赛信息不能为空");
        }
        if (refereeService.getById(refereeId) == null) {
            throw new BadRequestException("裁判不存在");
        }

        // 获取比赛
        Match match = matchService.getById(voMatch.getMatchId());
        if (match == null) {
            throw new BadRequestException("比赛不存在");
        }

        // 维护待更新的比赛信息
        match.setHomeTeamScore(voMatch.getHomeTeamScore());
        match.setAwayTeamScore(voMatch.getAwayTeamScore());
        match.setHomeTeamPenalty(voMatch.getHomeTeamPenalty());
        match.setAwayTeamPenalty(voMatch.getAwayTeamPenalty());
        match.setStatus(voMatch.getStatus());

        // 更新比赛结果
        if (!matchService.updateResult(refereeId, match)) {
            throw new InternalServerErrorException("更新比赛结果失败");
        }
    }

    @PostMapping("/referee/addPlayerAction")
    @Operation(summary = "添加比赛球员动作", description = "提供裁判 ID 和比赛球员动作信息，添加比赛球员动作")
    public void updatePlayerAction(Long refereeId, @RequestBody MatchPlayerAction matchPlayerAction) {
        if (refereeId == null || matchPlayerAction == null) {
            throw new BadRequestException("传参不能为空");
        }
        if (refereeService.getById(refereeId) == null) {
            throw new BadRequestException("裁判不存在");
        }
        if (matchPlayerAction.getMatchId() == null) {
            throw new BadRequestException("比赛ID不能为空");
        }
        if (matchService.getById(matchPlayerAction.getMatchId()) == null) {
            throw new BadRequestException("比赛不存在");
        }
        if (matchPlayerAction.getTeamId() == null) {
            throw new BadRequestException("球队ID不能为空");
        }
        if (teamService.getById(matchPlayerAction.getTeamId()) == null) {
            throw new BadRequestException("球队不存在");
        }
        if (matchPlayerAction.getPlayerId() == null) {
            throw new BadRequestException("球员ID不能为空");
        }
        if (playerService.getById(matchPlayerAction.getPlayerId()) == null) {
            throw new BadRequestException("球员不存在");
        }
        if (matchPlayerAction.getAction() == null) {
            throw new BadRequestException("动作不能为空");
        }
        if (matchPlayerAction.getTime() == null) {
            throw new BadRequestException("时间不能为空");
        }

        if (!matchService.addPlayerAction(refereeId, matchPlayerAction)) {
            throw new BadRequestException("更新比赛球员动作失败");
        }
    }

    @DeleteMapping("/referee/deletePlayerAction")
    @Operation(summary = "删除比赛球员动作", description = "提供裁判 ID 和比赛球员动作信息，删除比赛球员动作")
    public void deletePlayerAction(Long refereeId, @RequestBody MatchPlayerAction matchPlayerAction) {
        if (refereeId == null || matchPlayerAction == null) {
            throw new BadRequestException("传参不能为空");
        }
        if (refereeService.getById(refereeId) == null) {
            throw new BadRequestException("裁判不存在");
        }
        if (!matchService.deletePlayerAction(refereeId, matchPlayerAction)) {
            throw new BadRequestException("删除比赛球员动作失败");
        }
    }

    @GetMapping("/getEvent")
    @Operation(summary = "获取比赛所属赛事", description = "提供比赛 ID，获取比赛所属赛事")
    public Event getEvent(Long matchId) {
        if (matchId == null) {
            throw new BadRequestException("比赛ID不能为空");
        }
        if (matchService.getById(matchId) == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return matchService.getEvent(matchId);
    }

    @PostMapping("/live/add")
    @Operation(summary = "添加直播", description = "提供比赛 ID 和直播名称和直播地址，添加直播信息（链接）")
    @Parameters({
            @Parameter(name = "matchId", description = "比赛 ID", required = true),
            @Parameter(name = "liveName", description = "直播名称", required = true),
            @Parameter(name = "liveUrl", description = "直播地址", required = true)
    })
    public MatchLive addLive(Long matchId, String liveName, String liveUrl) {
        if (matchId == null || liveName == null || liveUrl == null) {
            throw new BadRequestException("比赛ID和直播名称和直播地址不能为空");
        }
        if (matchService.getById(matchId) == null) {
            throw new BadRequestException("比赛不存在");
        }
        MatchLive matchLive = new MatchLive(null, matchId, liveName, liveUrl);
        matchLiveService.save(matchLive);
        return matchLive;
    }

    @PutMapping("/live/update")
    @Operation(summary = "更新直播信息", description = "提供直播信息，更新直播信息（链接）")
    @Parameter(name = "matchLive", description = "直播信息", required = true)
    public void updateLive(@RequestBody MatchLive matchLive) {
        if (matchLive == null) {
            throw new BadRequestException("直播信息不能为空");
        }
        if (matchLive.getLiveId() == null) {
            throw new BadRequestException("更新的直播, 其ID不能为空");
        }
        if (!matchLiveService.updateById(matchLive)) {
            throw new BadRequestException("更新直播失败");
        }
    }

    @DeleteMapping("/live/delete")
    @Operation(summary = "删除直播", description = "提供直播 ID，删除直播信息（链接）")
    @Parameter(name = "liveId", description = "直播 ID", required = true)
    public void deleteLive(Long liveId) {
        if (liveId == null) {
            throw new BadRequestException("直播ID不能为空");
        }
        if (!matchLiveService.removeById(liveId)) {
            throw new BadRequestException("删除直播失败");
        }
    }

    @GetMapping("/live/get")

    public MatchLive getLive(Long liveId) {
        if (liveId == null) {
            throw new BadRequestException("ID不能为空");
        }
        MatchLive matchLive = matchLiveService.getById(liveId);
        if (matchLive == null) {
            throw new ResourceNotFoundException("直播源不存在");
        }
        return matchLive;
    }

    @GetMapping("/live/getAll")
    @Operation(summary = "获取所有直播", description = "提供比赛 ID，获取比赛的所有直播信息")
    @Parameter(name = "matchId", description = "比赛 ID", required = true)
    public List<MatchLive> getAllLives(Long matchId) {
        if (matchId == null) {
            throw new BadRequestException("比赛ID不能为空");
        }
        if (matchService.getById(matchId) == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        QueryWrapper<MatchLive> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId);
        return matchLiveService.list(queryWrapper);
    }

    @PostMapping("/video/add")
    @Operation(summary = "添加视频", description = "提供比赛 ID 和视频名称和视频地址，添加视频信息（链接）")
    @Parameters({
            @Parameter(name = "matchId", description = "比赛 ID", required = true),
            @Parameter(name = "videoName", description = "视频名称", required = true),
            @Parameter(name = "videoUrl", description = "视频地址", required = true)
    })
    public MatchVideo addVideo(Long matchId, String videoName, String videoUrl) {
        if (matchId == null || videoName == null || videoUrl == null) {
            throw new BadRequestException("比赛ID和视频名称和视频地址不能为空");
        }
        if (matchService.getById(matchId) == null) {
            throw new BadRequestException("比赛不存在");
        }
        MatchVideo matchVideo = new MatchVideo(null, matchId, videoName, videoUrl);
        matchVideoService.save(matchVideo);
        return matchVideo;
    }

    @PutMapping("/video/update")
    @Operation(summary = "更新视频信息", description = "提供视频信息，更新视频信息（链接）")
    @Parameter(name = "matchVideo", description = "视频信息", required = true)
    public void updateVideo(@RequestBody MatchVideo matchVideo) {
        if (matchVideo == null) {
            throw new BadRequestException("视频信息不能为空");
        }
        if (matchVideo.getVideoId() == null) {
            throw new BadRequestException("更新的视频, 其ID不能为空");
        }
        if (!matchVideoService.updateById(matchVideo)) {
            throw new BadRequestException("更新视频失败");
        }
    }

    @DeleteMapping("/video/delete")
    @Operation(summary = "删除视频", description = "提供视频 ID，删除视频信息（链接）")
    @Parameter(name = "videoId", description = "视频 ID", required = true)
    public void deleteVideo(Long videoId) {
        if (videoId == null) {
            throw new BadRequestException("视频ID不能为空");
        }
        if (!matchVideoService.removeById(videoId)) {
            throw new BadRequestException("删除视频失败");
        }
    }

    @GetMapping("/video/get")
    @Operation(summary = "获取视频", description = "提供视频 ID，获取视频信息（链接）")
    @Parameter(name = "videoId", description = "视频 ID", required = true)
    public MatchVideo getVideo(Long videoId) {
        if (videoId == null) {
            throw new BadRequestException("ID不能为空");
        }
        MatchVideo matchVideo = matchVideoService.getById(videoId);
        if (matchVideo == null) {
            throw new ResourceNotFoundException("视频源不存在");
        }
        return matchVideo;
    }

    @GetMapping("/video/getAll")
    @Operation(summary = "获取所有视频", description = "提供比赛 ID，获取比赛的所有视频信息")
    @Parameter(name = "matchId", description = "比赛 ID", required = true)
    public List<MatchVideo> getAllVideos(Long matchId) {
        if (matchId == null) {
            throw new BadRequestException("比赛ID不能为空");
        }
        if (matchService.getById(matchId) == null) {
            throw new BadRequestException("比赛不存在");
        }
        QueryWrapper<MatchVideo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId);
        return matchVideoService.list(queryWrapper);
    }
}
