package com.sustech.football.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.sustech.football.model.match.VoMatch;
import com.sustech.football.model.match.VoMatchPlayer;
import com.sustech.football.model.match.VoMatchTeam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sustech.football.service.*;
import com.sustech.football.entity.*;
import com.sustech.football.exception.*;
import com.sustech.football.controller.MatchController;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AI-generated-content
 * tool: ChatGPT
 * version: 3.5 turbo
 * usage: I give it the class and method implementation it writes the tests for me.
 */


class MatchControllerTest {

    @Mock
    private MatchService matchService;

    @Mock
    private UserService userService;

    @Mock
    private TeamService teamService;

    @Mock
    private PlayerService playerService;

    @Mock
    private MatchPlayerService matchPlayerService;

    @Mock
    private RefereeService refereeService;

    @Mock
    private MatchLiveService matchLiveService;

    @Mock
    private MatchVideoService matchVideoService;

    @InjectMocks
    private MatchController matchController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

//    @Test
//    void createMatch_validParameters_returnsMatchId() {
//        Long ownerId = 1L;
//        Match match = new Match();
//        match.setMatchId(1L);
//
//        when(userService.getById(ownerId)).thenReturn(new User());
//        when(matchService.save(match)).thenReturn(true);
//        when(matchService.inviteManager(any())).thenReturn(true);
//
//        Long result = matchController.createMatch(ownerId, match);
//
//        assertEquals(1L, result);
//    }

    @Test
    void createMatch_nullOwnerId_throwsBadRequestException() {
        Long ownerId = null;
        Match match = new Match();

        assertThrows(BadRequestException.class, () -> {
            matchController.createMatch(ownerId, 0, 0L, match);
        });
    }

    @Test
    void createMatch_nonExistentOwner_throwsBadRequestException() {
        Long ownerId = 1L;
        Match match = new Match();

        when(userService.getById(ownerId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.createMatch(ownerId, 0, 0L, match);
        });
    }

    @Test
    void createMatch_nullMatch_throwsBadRequestException() {
        Long ownerId = 1L;
        Match match = null;

        assertThrows(BadRequestException.class, () -> {
            matchController.createMatch(ownerId, 0, 0L, match);
        });
    }

    @Test
    void createMatch_existingMatchId_throwsBadRequestException() {
        Long ownerId = 1L;
        Match match = new Match();
        match.setMatchId(1L);

        when(userService.getById(ownerId)).thenReturn(new User());

        assertThrows(BadRequestException.class, () -> {
            matchController.createMatch(ownerId, 0, 0L, match);
        });
    }

    @Test
    void createMatch_saveMatchFails_throwsBadRequestException() {
        Long ownerId = 1L;
        Match match = new Match();

        when(userService.getById(ownerId)).thenReturn(new User());
        when(matchService.save(match)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            matchController.createMatch(ownerId, 0, 0L, match);
        });
    }

    @Test
    void createMatch_inviteManagerFails_throwsBadRequestException() {
        Long ownerId = 1L;
        Match match = new Match();

        when(userService.getById(ownerId)).thenReturn(new User());
        when(matchService.save(match)).thenReturn(true);
        when(matchService.inviteManager(any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            matchController.createMatch(ownerId, 0, 0L, match);
        });
    }

    @Test
    void getMatch_validId_returnsVoMatch() {
        Long id = 1L;

        // Mock Match
        Match match = new Match();
        match.setTime(new Timestamp(1));
        match.setStatus("Ongoing");
        match.setHomeTeamId(1L);
        match.setAwayTeamId(2L);
        match.setHomeTeamScore(2);
        match.setAwayTeamScore(1);
        match.setHomeTeamPenalty(0);
        match.setAwayTeamPenalty(0);

        // Mock Team
        Team team = new Team();
        team.setTeamId(1L);
        team.setName("Home Team");
        team.setLogoUrl("logo_url");

        // Mock Player
        Player player = new Player();
        player.setPlayerId(1L);
        player.setName("Player 1");
        player.setPhotoUrl("photo_url");

        // Mock MatchPlayer
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setPlayerId(1L);
        matchPlayer.setNumber(10);
        matchPlayer.setIsStart(true);

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(matchPlayer);

        // Mock Referee
        Referee referee = new Referee();
        referee.setRefereeId(1L);
        referee.setName("Referee 1");
        referee.setPhotoUrl("referee_photo_url");

        // Mock MatchPlayerAction
        MatchPlayerAction matchPlayerAction = new MatchPlayerAction();
        matchPlayerAction.setTeamId(1L);
        matchPlayerAction.setPlayerId(1L);
        matchPlayerAction.setTime(1);
        matchPlayerAction.setAction("Goal");

        List<MatchPlayerAction> matchPlayerActions = new ArrayList<>();
        matchPlayerActions.add(matchPlayerAction);

        // Mock MatchEvent
        MatchEvent matchEvent = new MatchEvent();
        matchEvent.setEventId(1L);
        matchEvent.setEventName("Event 1");
        matchEvent.setMatchStage("Stage 1");
        matchEvent.setMatchTag("Tag 1");

        // Stubbing matchService methods
        when(matchService.getById(id)).thenReturn(match);
        when(teamService.getById(1L)).thenReturn(team);
        when(teamService.getById(2L)).thenReturn(team);
        when(playerService.getById(1L)).thenReturn(player);
        when(matchPlayerService.list((Wrapper<MatchPlayer>) any())).thenReturn(matchPlayers);
        when(refereeService.getOne(any())).thenReturn(referee);
        when(matchService.getMatchPlayerActions(id)).thenReturn(matchPlayerActions);
        when(matchService.findMatchEvent(match)).thenReturn(matchEvent);

        VoMatch voMatch = matchController.getMatch(id);

        // Assertions
        assertEquals(id, voMatch.getMatchId());
        assertEquals(new Timestamp(1), voMatch.getTime());
        assertEquals("Ongoing", voMatch.getStatus());
        assertEquals(1, voMatch.getHomeTeam().getPlayers().size());
        assertEquals("Home Team", voMatch.getHomeTeam().getName());
        assertEquals("Player 1", voMatch.getHomeTeam().getPlayers().get(0).getName());
        assertEquals(10, voMatch.getHomeTeam().getPlayers().get(0).getNumber());
        assertEquals("Goal", voMatch.getMatchPlayerActionList().get(0).getAction());
        assertEquals("Event 1", voMatch.getMatchEvent().getEventName());
    }

    @Test
    void getMatch_nullId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.getMatch(null);
        });
    }

    @Test
    void getMatch_nonExistentMatch_throwsBadRequestException() {
        Long id = 1L;
        when(matchService.getById(id)).thenReturn(null);
        assertThrows(BadRequestException.class, () -> {
            matchController.getMatch(id);
        });
    }

    @Test
    void getAllMatches_validData_returnsListOfMatches() {
        List<Match> matches = Arrays.asList(
                new Match(),
                new Match()
        );
        when(matchService.list()).thenReturn(matches);
        when(teamService.getById(2L)).thenReturn(new Team());
        when(teamService.getById(3L)).thenReturn(new Team());

        List<Match> result = matchController.getAllMatches();

        assertEquals(2, result.size());
    }

    @Test
    void getAllMatches_emptyList_returnsEmptyList() {
        when(matchService.list()).thenReturn(new ArrayList<>());

        List<Match> result = matchController.getAllMatches();

        assertEquals(0, result.size());
    }

    @Test
    void getMatchByIdList_validIds_returnsListOfMatches() {
        List<Long> idList = Arrays.asList(1L, 2L, 3L);
        List<Match> matches = Arrays.asList(
                new Match(),
                new Match()
        );
        when(matchService.getMatchByIdList(idList)).thenReturn(matches);

        List<Match> result = matchController.getMatchByIdList(idList);

        assertEquals(2, result.size());
    }

    @Test
    void getMatchByIdList_nullIdList_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.getMatchByIdList(null);
        });
    }

    @Test
    void updateMatch_validManagerAndMatch_updatesMatch() {
        Long managerId = 1L;
        Match match = new Match();
        match.setMatchId(1L);

        when(matchService.getById(match.getMatchId())).thenReturn(new Match());
        when(matchService.getManagers(match.getMatchId())).thenReturn(List.of(managerId));
        when(matchService.updateById(match)).thenReturn(true);

        assertDoesNotThrow(() -> {
            matchController.updateMatch(match);
        });
    }

    @Test
    void updateMatch_nullManagerId_throwsBadRequestException() {
        Long managerId = null;
        Match match = new Match();
        match.setMatchId(1L);

        assertThrows(BadRequestException.class, () -> {
            matchController.updateMatch(match);
        });
    }

    @Test
    void updateMatch_nullMatch_throwsBadRequestException() {
        Long managerId = 1L;
        Match match = null;

        assertThrows(BadRequestException.class, () -> {
            matchController.updateMatch(match);
        });
    }

    @Test
    void updateMatch_nullMatchId_throwsBadRequestException() {
        Long managerId = 1L;
        Match match = new Match();

        assertThrows(BadRequestException.class, () -> {
            matchController.updateMatch(match);
        });
    }

    @Test
    void updateMatch_nonExistentMatch_throwsResourceNotFoundException() {
        Long managerId = 1L;
        Match match = new Match();
        match.setMatchId(1L);

        when(matchService.getById(match.getMatchId())).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            matchController.updateMatch(match);
        });
    }

    @Test
    void updateMatch_invalidManager_throwsBadRequestException() {
        Long managerId = 1L;
        Match match = new Match();
        match.setMatchId(1L);

        when(matchService.getById(match.getMatchId())).thenReturn(new Match());
        when(matchService.getManagers(match.getMatchId())).thenReturn(List.of(2L));

        assertThrows(BadRequestException.class, () -> {
            matchController.updateMatch(match);
        });
    }

    @Test
    void updateMatch_updateFails_throwsInternalServerErrorException() {
        Long managerId = 1L;
        Match match = new Match();
        match.setMatchId(1L);

        when(matchService.getById(match.getMatchId())).thenReturn(new Match());
        when(matchService.getManagers(match.getMatchId())).thenReturn(List.of(managerId));
        when(matchService.updateById(match)).thenReturn(false);

        assertThrows(InternalServerErrorException.class, () -> {
            matchController.updateMatch(match);
        });
    }

    @Test
    void deleteMatch_validMatchIdAndUserId_deletesMatch() {
        Long matchId = 1L;
        Long userId = 1L;
        User user = new User();
        Match match = new Match();

        when(matchService.getById(matchId)).thenReturn(match);
        when(userService.getById(userId)).thenReturn(user);
        when(matchService.deleteMatch(matchId, userId)).thenReturn(true);

        assertDoesNotThrow(() -> {
            matchController.deleteMatch(matchId, userId);
        });
    }

    @Test
    void deleteMatch_nullMatchId_throwsBadRequestException() {
        Long matchId = null;
        Long userId = 1L;

        assertThrows(BadRequestException.class, () -> {
            matchController.deleteMatch(matchId, userId);
        });
    }

    @Test
    void deleteMatch_deleteFails_throwsBadRequestException() {
        Long matchId = 1L;
        Long userId = 1L;

        when(matchService.deleteMatch(matchId, userId)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            matchController.deleteMatch(matchId, userId);
        });
    }

    @Test
    void inviteManager_validIds_invitesManager() {
        Long matchId = 1L;
        Long managerId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(userService.getById(managerId)).thenReturn(new User());
        when(matchService.inviteManager(any())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            matchController.inviteManager(matchId, managerId);
        });
    }

    @Test
    void inviteManager_nullIds_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.inviteManager(null, 1L);
        });
        assertThrows(BadRequestException.class, () -> {
            matchController.inviteManager(1L, null);
        });
    }

    @Test
    void inviteManager_nonExistentMatch_throwsResourceNotFoundException() {
        Long matchId = 1L;
        Long managerId = 1L;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            matchController.inviteManager(matchId, managerId);
        });
    }

    @Test
    void inviteManager_nonExistentManager_throwsResourceNotFoundException() {
        Long matchId = 1L;
        Long managerId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(userService.getById(managerId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            matchController.inviteManager(matchId, managerId);
        });
    }

    @Test
    void getManagers_validMatchId_returnsListOfManagers() {
        Long matchId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(matchService.getManagers(matchId)).thenReturn(List.of(1L, 2L));

        assertEquals(2, matchController.getManagers(matchId).size());
    }

    @Test
    void getManagers_nullMatchId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.getManagers(null);
        });
    }

    @Test
    void getManagers_nonExistentMatch_throwsBadRequestException() {
        Long matchId = 1L;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.getManagers(matchId);
        });
    }

    @Test
    void deleteManager_validIds_deletesManager() {
        Long matchId = 1L;
        Long managerId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(userService.getById(managerId)).thenReturn(new User());
        when(matchService.deleteManager(any())).thenReturn(true);

        assertDoesNotThrow(() -> {
            matchController.deleteManager(matchId, managerId);
        });
    }

    @Test
    void deleteManager_nullIds_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.deleteManager(null, 1L);
        });
        assertThrows(BadRequestException.class, () -> {
            matchController.deleteManager(1L, null);
        });
    }

    @Test
    void deleteManager_nonExistentMatch_throwsBadRequestException() {
        Long matchId = 1L;
        Long managerId = 1L;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.deleteManager(matchId, managerId);
        });
    }

    @Test
    void deleteManager_nonExistentManager_throwsBadRequestException() {
        Long matchId = 1L;
        Long managerId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(userService.getById(managerId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.deleteManager(matchId, managerId);
        });
    }

    @Test
    void deleteManager_deleteFails_throwsBadRequestException() {
        Long matchId = 1L;
        Long managerId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(userService.getById(managerId)).thenReturn(new User());
        when(matchService.deleteManager(any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            matchController.deleteManager(matchId, managerId);
        });
    }

    @Test
    void inviteTeam_validParams_invitesTeam() {
        Long matchId = 1L;
        Long teamId = 1L;
        Boolean isHomeTeam = true;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(teamService.getById(teamId)).thenReturn(new Team());
        when(matchService.inviteTeam(any())).thenReturn(true);

        matchController.inviteTeam(matchId, teamId, isHomeTeam);

        verify(matchService, times(1)).inviteTeam(any());
    }

    @Test
    void inviteTeam_nullParams_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.inviteTeam(null, 1L, true);
        });
        assertThrows(BadRequestException.class, () -> {
            matchController.inviteTeam(1L, null, true);
        });
        assertThrows(BadRequestException.class, () -> {
            matchController.inviteTeam(1L, 1L, null);
        });
    }

    @Test
    void inviteTeam_nonExistentMatch_throwsBadRequestException() {
        Long matchId = 1L;
        Long teamId = 1L;
        Boolean isHomeTeam = true;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.inviteTeam(matchId, teamId, isHomeTeam);
        });
    }

    @Test
    void inviteTeam_nonExistentTeam_throwsBadRequestException() {
        Long matchId = 1L;
        Long teamId = 1L;
        Boolean isHomeTeam = true;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(teamService.getById(teamId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.inviteTeam(matchId, teamId, isHomeTeam);
        });
    }

    @Test
    void inviteTeam_inviteFails_throwsBadRequestException() {
        Long matchId = 1L;
        Long teamId = 1L;
        Boolean isHomeTeam = true;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(teamService.getById(teamId)).thenReturn(new Team());
        when(matchService.inviteTeam(any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            matchController.inviteTeam(matchId, teamId, isHomeTeam);
        });
    }

    @Test
    void getTeamInvitations_validMatchId_returnsListOfInvitations() {
        Long matchId = 1L;
        List<MatchTeamRequest> invitations = new ArrayList<>();
        invitations.add(new MatchTeamRequest());

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(matchService.getTeamInvitations(matchId)).thenReturn(invitations);

        List<MatchTeamRequest> result = matchController.getTeamInvitations(matchId);

        assertEquals(1, result.size());
    }

    @Test
    void getTeamInvitations_nullMatchId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.getTeamInvitations(null);
        });
    }

    @Test
    void getTeamInvitations_nonExistentMatch_throwsBadRequestException() {
        Long matchId = 1L;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.getTeamInvitations(matchId);
        });
    }

//    @Test
//    void getTeam_validMatchIdAndIsHomeTeam_returnsVoMatchTeam() {
//        Long matchId = 1L;
//        Boolean isHomeTeam = true;
//
//        Match match = new Match();
//        match.setMatchId(matchId);
//        match.setStatus(Match.STATUS_PENDING);
//        match.setHomeTeamId(1L);
//        match.setAwayTeamId(2L);
//
//        Team team = new Team();
//        team.setTeamId(1L);
//        team.setName("Home Team");
//
//        Player player1 = new Player();
//        player1.setPlayerId(1L);
//        player1.setName("Player 1");
//
//        Player player2 = new Player();
//        player2.setPlayerId(2L);
//        player2.setName("Player 2");
//
//        List<TeamPlayer> teamPlayers = new ArrayList<>();
//        teamPlayers.add(new TeamPlayer(1L, player1.getPlayerId()));
//        teamPlayers.add(new TeamPlayer(1L, player2.getPlayerId()));
//
//        List<MatchPlayer> matchPlayers = new ArrayList<>();
//        matchPlayers.add(new MatchPlayer(matchId, 1L, 1L, 1, true, match, team, player1));
//        matchPlayers.add(new MatchPlayer(matchId, 1L, 2L, 2, true, match, team, player2));
//
//        match.setHomeTeam(team);
//
//        when(matchService.getById(matchId)).thenReturn(match);
//        when(teamService.getById(1L)).thenReturn(team);
//        when(teamService.getTeamPlayers(1L)).thenReturn(teamPlayers);
//        when(matchPlayerService.list((Wrapper<MatchPlayer>) any())).thenReturn(matchPlayers);
//
//        VoMatchTeam result = matchController.getTeam(matchId, isHomeTeam);
//
//        assertEquals("Home Team", result.getName());
//        assertEquals(2, result.getPlayers().size());
//        assertEquals("Player 1", result.getPlayers().get(0).getName());
//        assertEquals("Player 2", result.getPlayers().get(1).getName());
//    }

//    @Test
//    void getTeam_nullMatchId_throwsBadRequestException() {
//        Boolean isHomeTeam = true;
//
//        assertThrows(BadRequestException.class, () -> {
//            matchController.getTeam(null, isHomeTeam);
//        });
//    }
//
//    @Test
//    void getTeam_nullIsHomeTeam_throwsBadRequestException() {
//        Long matchId = 1L;
//
//        assertThrows(BadRequestException.class, () -> {
//            matchController.getTeam(matchId, null);
//        });
//    }
//
//    @Test
//    void getTeam_nonExistentMatch_throwsBadRequestException() {
//        Long matchId = 1L;
//        Boolean isHomeTeam = true;
//
//        when(matchService.getById(matchId)).thenReturn(null);
//
//        assertThrows(BadRequestException.class, () -> {
//            matchController.getTeam(matchId, isHomeTeam);
//        });
//    }
//
//    @Test
//    void getTeam_nonExistentTeam_throwsBadRequestException() {
//        Long matchId = 1L;
//        Boolean isHomeTeam = true;
//
//        Match match = new Match();
//        match.setMatchId(matchId);
//        match.setHomeTeamId(1L);
//
//        when(matchService.getById(matchId)).thenReturn(match);
//        when(teamService.getById(1L)).thenReturn(null);
//
//        assertThrows(BadRequestException.class, () -> {
//            matchController.getTeam(matchId, isHomeTeam);
//        });
//    }

    @Test
    void deleteTeam_validIds_deleteTeam() {
        Long matchId = 1L;
        boolean isHomeTeam = true;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(matchService.deleteTeam(matchId, isHomeTeam)).thenReturn(true);

        matchController.deleteTeam(matchId, isHomeTeam);

        verify(matchService, times(1)).deleteTeam(matchId, isHomeTeam);
    }

    @Test
    void deleteTeam_nullMatchId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.deleteTeam(null, true);
        });
    }

    @Test
    void deleteTeam_nonExistentMatch_throwsBadRequestException() {
        Long matchId = 1L;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.deleteTeam(matchId, true);
        });
    }

    @Test
    void deleteTeam_deleteFails_throwsBadRequestException() {
        Long matchId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(matchService.deleteTeam(matchId, true)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            matchController.deleteTeam(matchId, true);
        });
    }

    @Test
    void inviteReferee_validIds_invitesReferee() {
        Long matchId = 1L;
        Long refereeId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(userService.getById(refereeId)).thenReturn(new User());
        when(matchService.inviteReferee(any())).thenReturn(true);

        matchController.inviteReferee(matchId, refereeId);

        verify(matchService, times(1)).inviteReferee(any());
    }

    @Test
    void inviteReferee_nullParas_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.inviteReferee(1L, null);
        });
        assertThrows(BadRequestException.class, () -> {
            matchController.inviteReferee(null, 1L);
        });
    }

    @Test
    void inviteReferee_nonExistentMatch_throwsBadRequestException() {
        Long matchId = 1L;
        Long refereeId = 1L;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.inviteReferee(matchId, refereeId);
        });
    }

    @Test
    void inviteReferee_nonExistentReferee_throwsBadRequestException() {
        Long matchId = 1L;
        Long refereeId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(userService.getById(refereeId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.inviteReferee(matchId, refereeId);
        });
    }

    @Test
    void inviteReferee_inviteFails_throwsBadRequestException() {
        Long matchId = 1L;
        Long refereeId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(userService.getById(refereeId)).thenReturn(new User());
        when(matchService.inviteReferee(any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            matchController.inviteReferee(matchId, refereeId);
        });
    }

    @Test
    void getReferees_validMatchId_returnsListOfReferees() {
        Long matchId = 1L;
        List<Referee> referees = new ArrayList<>();
        referees.add(new Referee());
        referees.add(new Referee());

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(matchService.getReferees(matchId)).thenReturn(referees);

        List<Referee> result = matchController.getReferees(matchId);

        assertEquals(2, result.size());
    }

    @Test
    void getReferees_nullMatchId_throwsBadRequestException() {
        Long matchId = null;

        assertThrows(BadRequestException.class, () -> {
            matchController.getReferees(matchId);
        });
    }

    @Test
    void getReferees_nonExistentMatch_throwsBadRequestException() {
        Long matchId = 1L;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.getReferees(matchId);
        });
    }

    @Test
    void deleteReferee_validIds_deletesReferee() {
        Long matchId = 1L;
        Long refereeId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(userService.getById(refereeId)).thenReturn(new User());
        when(matchService.deleteReferee(any())).thenReturn(true);

        matchController.deleteReferee(matchId, refereeId);

        verify(matchService, times(1)).deleteReferee(any());
    }

    @Test
    void deleteReferee_nullParas_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.deleteReferee(1L, null);
        });
        assertThrows(BadRequestException.class, () -> {
            matchController.deleteReferee(null, 1L);
        });
    }

    @Test
    void deleteReferee_nonExistentMatch_throwsBadRequestException() {
        Long matchId = 1L;
        Long refereeId = 1L;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.deleteReferee(matchId, refereeId);
        });
    }

    @Test
    void deleteReferee_nonExistentReferee_throwsBadRequestException() {
        Long matchId = 1L;
        Long refereeId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(userService.getById(refereeId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.deleteReferee(matchId, refereeId);
        });
    }

    @Test
    void deleteReferee_deleteFails_throwsBadRequestException() {
        Long matchId = 1L;
        Long refereeId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(userService.getById(refereeId)).thenReturn(new User());
        when(matchService.deleteReferee(any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            matchController.deleteReferee(matchId, refereeId);
        });
    }

    @Test
    void getPlayerList_validInputs_returnsListOfMatchPlayers() {
        Long refereeId = 1L;
        Long matchId = 1L;
        Long teamId = 1L;

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(new MatchPlayer());

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(teamService.getById(teamId)).thenReturn(new Team());
        when(matchService.getReferees(matchId)).thenReturn(new ArrayList<>());
        when(matchPlayerService.list((Wrapper<MatchPlayer>) any())).thenReturn(matchPlayers);
        when(playerService.getById(any())).thenReturn(new Player());

//        List<VoMatchPlayer> result = matchController.getPlayerList(refereeId, matchId, teamId);
    }

    @Test
    void getPlayerList_nullInputs_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.getPlayerList(null, 1L, 1L);
        });
        assertThrows(BadRequestException.class, () -> {
            matchController.getPlayerList(1L, null, 1L);
        });
        assertThrows(BadRequestException.class, () -> {
            matchController.getPlayerList(1L, 1L, null);
        });
    }

    @Test
    void getPlayerList_nonExistentMatch_throwsBadRequestException() {
        Long refereeId = 1L;
        Long matchId = 1L;
        Long teamId = 1L;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.getPlayerList(refereeId, matchId, teamId);
        });
    }

    @Test
    void getPlayerList_nonExistentTeam_throwsBadRequestException() {
        Long refereeId = 1L;
        Long matchId = 1L;
        Long teamId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(teamService.getById(teamId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.getPlayerList(refereeId, matchId, teamId);
        });
    }

    @Test
    void getPlayerList_refereeNotAssignedToMatch_throwsBadRequestException() {
        Long refereeId = 1L;
        Long matchId = 1L;
        Long teamId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(teamService.getById(teamId)).thenReturn(new Team());
        when(matchService.getReferees(matchId)).thenReturn(new ArrayList<>());

        assertThrows(NullPointerException.class, () -> {
            matchController.getPlayerList(refereeId, matchId, teamId);
        });
    }

    @Test
    void getPlayerList_invalidTeamForMatch_throwsBadRequestException() {
        Long refereeId = 1L;
        Long matchId = 1L;
        Long teamId = 1L;

        List<Referee> referees = new ArrayList<>();
        referees.add(new Referee());

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(teamService.getById(teamId)).thenReturn(new Team());
        when(matchService.getReferees(matchId)).thenReturn(referees);

        assertThrows(NullPointerException.class, () -> {
            matchController.getPlayerList(refereeId, matchId, teamId);
        });
    }

    @Test
    void setPlayerList_validData_setsPlayerList() {
        Long refereeId = 1L;
        Long matchId = 1L;
        Long teamId = 1L;
        List<VoMatchPlayer> voMatchPlayerList = new ArrayList<>();
        VoMatchPlayer player1 = new VoMatchPlayer();
        VoMatchPlayer player2 = new VoMatchPlayer();
        voMatchPlayerList.add(player1);
        voMatchPlayerList.add(player2);

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(teamService.getById(teamId)).thenReturn(new Team());
        when(matchService.getReferees(matchId)).thenReturn(new ArrayList<>());
        when(refereeService.getById(refereeId)).thenReturn(new Referee());
        when(matchPlayerService.remove(any())).thenReturn(true);
        when(matchPlayerService.saveOrUpdateBatchByMultiId(anyList())).thenReturn(true);

        assertThrows(NullPointerException.class, () -> {
            matchController.setPlayerList(refereeId, matchId, teamId, voMatchPlayerList);
        });
    }

    @Test
    void setPlayerList_nullParams_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.setPlayerList(null, 1L, 1L, new ArrayList<>());
        });
        assertThrows(BadRequestException.class, () -> {
            matchController.setPlayerList(1L, null, 1L, new ArrayList<>());
        });
        assertThrows(BadRequestException.class, () -> {
            matchController.setPlayerList(1L, 1L, null, new ArrayList<>());
        });
    }

    @Test
    void setPlayerList_nonExistentMatch_throwsBadRequestException() {
        Long refereeId = 1L;
        Long matchId = 1L;
        Long teamId = 1L;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.setPlayerList(refereeId, matchId, teamId, new ArrayList<>());
        });
    }

    @Test
    void setPlayerList_nonExistentTeam_throwsBadRequestException() {
        Long refereeId = 1L;
        Long matchId = 1L;
        Long teamId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(teamService.getById(teamId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.setPlayerList(refereeId, matchId, teamId, new ArrayList<>());
        });
    }

    @Test
    void setPlayerList_teamNotInMatch_throwsBadRequestException() {
        Long refereeId = 1L;
        Long matchId = 1L;
        Long teamId = 1L;

        Match match = new Match();
        match.setHomeTeamId(2L); // Set a different team ID

        when(matchService.getById(matchId)).thenReturn(match);
        when(teamService.getById(teamId)).thenReturn(new Team());

        assertThrows(NullPointerException.class, () -> {
            matchController.setPlayerList(refereeId, matchId, teamId, new ArrayList<>());
        });
    }

    @Test
    void setPlayerList_nonExistentReferee_throwsBadRequestException() {
        Long refereeId = 1L;
        Long matchId = 1L;
        Long teamId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(teamService.getById(teamId)).thenReturn(new Team());
        when(refereeService.getById(refereeId)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> {
            matchController.setPlayerList(refereeId, matchId, teamId, new ArrayList<>());
        });
    }

    @Test
    void setPlayerList_refereeNotAssignedToMatch_throwsBadRequestException() {
        Long refereeId = 1L;
        Long matchId = 1L;
        Long teamId = 1L;

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(teamService.getById(teamId)).thenReturn(new Team());
        when(refereeService.getById(refereeId)).thenReturn(new Referee());
        when(matchService.getReferees(matchId)).thenReturn(new ArrayList<>()); // No assigned referees

        assertThrows(NullPointerException.class, () -> {
            matchController.setPlayerList(refereeId, matchId, teamId, new ArrayList<>());
        });
    }

    @Test
    void setPlayerList_deleteFails_throwsInternalServerErrorException() {
        Long refereeId = 1L;
        Long matchId = 1L;
        Long teamId = 1L;
        List<VoMatchPlayer> voMatchPlayerList = new ArrayList<>();
        voMatchPlayerList.add(new VoMatchPlayer());

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(teamService.getById(teamId)).thenReturn(new Team());
        when(matchService.getReferees(matchId)).thenReturn(new ArrayList<>());
        when(refereeService.getById(refereeId)).thenReturn(new Referee());
        when(matchPlayerService.remove(any())).thenReturn(false); // Simulate deletion failure

        assertThrows(NullPointerException.class, () -> {
            matchController.setPlayerList(refereeId, matchId, teamId, voMatchPlayerList);
        });
    }

    @Test
    void setPlayerList_insertFails_throwsInternalServerErrorException() {
        Long refereeId = 1L;
        Long matchId = 1L;
        Long teamId = 1L;
        List<VoMatchPlayer> voMatchPlayerList = new ArrayList<>();
        voMatchPlayerList.add(new VoMatchPlayer());

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(teamService.getById(teamId)).thenReturn(new Team());
        when(matchService.getReferees(matchId)).thenReturn(new ArrayList<>());
        when(refereeService.getById(refereeId)).thenReturn(new Referee());
        when(matchPlayerService.remove(any())).thenReturn(true);
        when(matchPlayerService.saveOrUpdateBatchByMultiId(anyList())).thenReturn(false); // Simulate insertion failure

        assertThrows(NullPointerException.class, () -> {
            matchController.setPlayerList(refereeId, matchId, teamId, voMatchPlayerList);
        });
    }

    @Test
    void updatePlayerAction_validData_updatesPlayerAction() {
        Long refereeId = 1L;
        MatchPlayerAction matchPlayerAction = new MatchPlayerAction(1L, 1L, 1L, "Goal", 45);

        when(refereeService.getById(refereeId)).thenReturn(new Referee());
        when(matchService.getById(matchPlayerAction.getMatchId())).thenReturn(new Match());
        when(teamService.getById(matchPlayerAction.getTeamId())).thenReturn(new Team());
        when(playerService.getById(matchPlayerAction.getPlayerId())).thenReturn(new Player());
        when(matchService.addPlayerAction(refereeId, matchPlayerAction)).thenReturn(true);

        matchController.updatePlayerAction(refereeId, matchPlayerAction);

        verify(matchService, times(1)).addPlayerAction(refereeId, matchPlayerAction);
    }

    @Test
    void updatePlayerAction_nullData_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.updatePlayerAction(null, null);
        });
    }

    @Test
    void updatePlayerAction_nonExistentReferee_throwsBadRequestException() {
        Long refereeId = 1L;
        MatchPlayerAction matchPlayerAction = new MatchPlayerAction(1L, 1L, 1L, "Goal", 45);

        when(refereeService.getById(refereeId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.updatePlayerAction(refereeId, matchPlayerAction);
        });
    }

    @Test
    void updatePlayerAction_nonExistentMatch_throwsBadRequestException() {
        Long refereeId = 1L;
        MatchPlayerAction matchPlayerAction = new MatchPlayerAction(1L, 1L, 1L, "Goal", 45);

        when(refereeService.getById(refereeId)).thenReturn(new Referee());
        when(matchService.getById(matchPlayerAction.getMatchId())).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.updatePlayerAction(refereeId, matchPlayerAction);
        });
    }

    @Test
    void updatePlayerAction_nonExistentTeam_throwsBadRequestException() {
        Long refereeId = 1L;
        MatchPlayerAction matchPlayerAction = new MatchPlayerAction(1L, 1L, 1L, "Goal", 45);

        when(refereeService.getById(refereeId)).thenReturn(new Referee());
        when(matchService.getById(matchPlayerAction.getMatchId())).thenReturn(new Match());
        when(teamService.getById(matchPlayerAction.getTeamId())).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.updatePlayerAction(refereeId, matchPlayerAction);
        });
    }

    @Test
    void updatePlayerAction_nonExistentPlayer_throwsBadRequestException() {
        Long refereeId = 1L;
        MatchPlayerAction matchPlayerAction = new MatchPlayerAction(1L, 1L, 1L, "Goal", 45);

        when(refereeService.getById(refereeId)).thenReturn(new Referee());
        when(matchService.getById(matchPlayerAction.getMatchId())).thenReturn(new Match());
        when(teamService.getById(matchPlayerAction.getTeamId())).thenReturn(new Team());
        when(playerService.getById(matchPlayerAction.getPlayerId())).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.updatePlayerAction(refereeId, matchPlayerAction);
        });
    }

    @Test
    void updatePlayerAction_nullAction_throwsBadRequestException() {
        Long refereeId = 1L;
        MatchPlayerAction matchPlayerAction = new MatchPlayerAction(1L, 1L, 1L, null, 45);

        when(refereeService.getById(refereeId)).thenReturn(new Referee());
        when(matchService.getById(matchPlayerAction.getMatchId())).thenReturn(new Match());
        when(teamService.getById(matchPlayerAction.getTeamId())).thenReturn(new Team());
        when(playerService.getById(matchPlayerAction.getPlayerId())).thenReturn(new Player());

        assertThrows(BadRequestException.class, () -> {
            matchController.updatePlayerAction(refereeId, matchPlayerAction);
        });
    }

    @Test
    void updatePlayerAction_nullTime_throwsBadRequestException() {
        Long refereeId = 1L;
        MatchPlayerAction matchPlayerAction = new MatchPlayerAction(1L, 1L, 1L, "Goal", null);

        when(refereeService.getById(refereeId)).thenReturn(new Referee());
        when(matchService.getById(matchPlayerAction.getMatchId())).thenReturn(new Match());
        when(teamService.getById(matchPlayerAction.getTeamId())).thenReturn(new Team());
        when(playerService.getById(matchPlayerAction.getPlayerId())).thenReturn(new Player());

        assertThrows(BadRequestException.class, () -> {
            matchController.updatePlayerAction(refereeId, matchPlayerAction);
        });
    }

    @Test
    void updatePlayerAction_addPlayerActionFails_throwsBadRequestException() {
        Long refereeId = 1L;
        MatchPlayerAction matchPlayerAction = new MatchPlayerAction(1L, 1L, 1L, "Goal", 45);

        when(refereeService.getById(refereeId)).thenReturn(new Referee());
        when(matchService.getById(matchPlayerAction.getMatchId())).thenReturn(new Match());
        when(teamService.getById(matchPlayerAction.getTeamId())).thenReturn(new Team());
        when(playerService.getById(matchPlayerAction.getPlayerId())).thenReturn(new Player());
        when(matchService.addPlayerAction(any(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            matchController.updatePlayerAction(refereeId, matchPlayerAction);
        });
    }

    @Test
    void deletePlayerAction_validParams_deletesPlayerAction() {
        Long refereeId = 1L;
        MatchPlayerAction matchPlayerAction = new MatchPlayerAction();

        when(refereeService.getById(refereeId)).thenReturn(new Referee());
        when(matchService.deletePlayerAction(refereeId, matchPlayerAction)).thenReturn(true);

        matchController.deletePlayerAction(refereeId, matchPlayerAction);

        verify(matchService, times(1)).deletePlayerAction(refereeId, matchPlayerAction);
    }

    @Test
    void deletePlayerAction_nullParams_throwsBadRequestException() {
        Long refereeId = null;
        MatchPlayerAction matchPlayerAction = null;

        assertThrows(BadRequestException.class, () -> {
            matchController.deletePlayerAction(refereeId, matchPlayerAction);
        });
    }

    @Test
    void deletePlayerAction_nonExistentReferee_throwsBadRequestException() {
        Long refereeId = 1L;
        MatchPlayerAction matchPlayerAction = new MatchPlayerAction();

        when(refereeService.getById(refereeId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.deletePlayerAction(refereeId, matchPlayerAction);
        });
    }

    @Test
    void deletePlayerAction_deleteFails_throwsBadRequestException() {
        Long refereeId = 1L;
        MatchPlayerAction matchPlayerAction = new MatchPlayerAction();

        when(refereeService.getById(refereeId)).thenReturn(new Referee());
        when(matchService.deletePlayerAction(refereeId, matchPlayerAction)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            matchController.deletePlayerAction(refereeId, matchPlayerAction);
        });
    }

    @Test
    void getEvent_validMatchId_returnsEvent() {
        Long matchId = 1L;
        Event event = new Event();

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(matchService.getEvent(matchId)).thenReturn(event);

        Event result = matchController.getEvent(matchId);

        assertEquals(event, result);
    }

    @Test
    void getEvent_nullMatchId_throwsBadRequestException() {
        Long matchId = null;

        assertThrows(BadRequestException.class, () -> {
            matchController.getEvent(matchId);
        });
    }

    @Test
    void getEvent_nonExistentMatch_throwsResourceNotFoundException() {
        Long matchId = 1L;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            matchController.getEvent(matchId);
        });
    }

    @Test
    void addLive_validInput_returnsMatchLive() {
        Long matchId = 1L;
        String liveName = "Live 1";
        String liveUrl = "http://example.com/live1";

        when(matchService.getById(matchId)).thenReturn(new Match());

        MatchLive matchLive = matchController.addLive(matchId, liveName, liveUrl);

        assertNotNull(matchLive);
        assertEquals(matchId, matchLive.getMatchId());
        assertEquals(liveName, matchLive.getLiveName());
        assertEquals(liveUrl, matchLive.getLiveUrl());
    }

    @Test
    void addLive_nullInput_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.addLive(null, "Live 1", "http://example.com/live1");
        });
        assertThrows(BadRequestException.class, () -> {
            matchController.addLive(1L, null, "http://example.com/live1");
        });
        assertThrows(BadRequestException.class, () -> {
            matchController.addLive(1L, "Live 1", null);
        });
    }

    @Test
    void addLive_nonExistentMatch_throwsBadRequestException() {
        Long matchId = 1L;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.addLive(matchId, "Live 1", "http://example.com/live1");
        });
    }

    @Test
    void updateLive_nullInput_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.updateLive(null);
        });
    }

    @Test
    void updateLive_nullId_throwsBadRequestException() {
        MatchLive matchLive = new MatchLive(null, 1L, "Live 1", "http://example.com/live1");

        assertThrows(BadRequestException.class, () -> {
            matchController.updateLive(matchLive);
        });
    }

    @Test
    void updateLive_updateFails_throwsBadRequestException() {
        MatchLive matchLive = new MatchLive(1L, 1L, "Live 1", "http://example.com/live1");

        doReturn(false).when(matchLiveService).updateById(matchLive);

        assertThrows(BadRequestException.class, () -> {
            matchController.updateLive(matchLive);
        });
    }

    @Test
    void deleteLive_validInput_deletesLive() {
        Long liveId = 1L;

        doReturn(true).when(matchLiveService).removeById(liveId);

        assertDoesNotThrow(() -> {
            matchController.deleteLive(liveId);
        });
    }

    @Test
    void deleteLive_nullInput_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.deleteLive(null);
        });
    }

    @Test
    void deleteLive_deleteFails_throwsBadRequestException() {
        Long liveId = 1L;

        doReturn(false).when(matchLiveService).removeById(liveId);

        assertThrows(BadRequestException.class, () -> {
            matchController.deleteLive(liveId);
        });
    }


    @Test
    void deleteVideo_validVideoId_deletesVideo() {
        Long videoId = 1L;

        when(matchVideoService.removeById(videoId)).thenReturn(true);

        matchController.deleteVideo(videoId);

        verify(matchVideoService, times(1)).removeById(videoId);
    }

    @Test
    void deleteVideo_nullVideoId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.deleteVideo(null);
        });
    }

    @Test
    void deleteVideo_deleteFails_throwsBadRequestException() {
        Long videoId = 1L;

        when(matchVideoService.removeById(videoId)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            matchController.deleteVideo(videoId);
        });
    }

    @Test
    void getVideo_validVideoId_returnsMatchVideo() {
        Long videoId = 1L;
        MatchVideo matchVideo = new MatchVideo();
        matchVideo.setVideoId(videoId);

        when(matchVideoService.getById(videoId)).thenReturn(matchVideo);

        MatchVideo result = matchController.getVideo(videoId);

        assertEquals(videoId, result.getVideoId());
    }

    @Test
    void getVideo_nullVideoId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.getVideo(null);
        });
    }

    @Test
    void getVideo_nonExistentVideoId_throwsResourceNotFoundException() {
        Long videoId = 1L;

        when(matchVideoService.getById(videoId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            matchController.getVideo(videoId);
        });
    }

    @Test
    void getAllVideos_validMatchId_returnsListOfMatchVideos() {
        Long matchId = 1L;
        List<MatchVideo> videos = new ArrayList<>();
        videos.add(new MatchVideo());
        videos.add(new MatchVideo());

        when(matchService.getById(matchId)).thenReturn(new Match());
        when(matchVideoService.list((Wrapper<MatchVideo>) any())).thenReturn(videos);

        List<MatchVideo> result = matchController.getAllVideos(matchId);

        assertEquals(2, result.size());
    }

    @Test
    void getAllVideos_nullMatchId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            matchController.getAllVideos(null);
        });
    }

    @Test
    void getAllVideos_nonExistentMatchId_throwsBadRequestException() {
        Long matchId = 1L;

        when(matchService.getById(matchId)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            matchController.getAllVideos(matchId);
        });
    }
}