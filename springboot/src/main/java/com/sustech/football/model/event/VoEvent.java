package com.sustech.football.model.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
public class VoEvent {
    Long eventId;
    String name;
    String description;
    Integer matchPlayerCount;
    Integer rosterSize;
    List<VoManager> managerList;
    List<VoTeamInfo> teamList;
    List<VoGroup> groupList;
    List<VoMatch> matchList;
    List<Stage> stageList;

    @Data
    @NoArgsConstructor
    public static class VoManager {
        Long userId;
        String nickName;
        String avatarUrl;
    }

    @Data
    @NoArgsConstructor
    public static class VoTeamInfo {
        Long id;
        String name;
        String logoUrl;
        Integer playerCount;
    }

    @Data
    @NoArgsConstructor
    public static class VoGroup {
        @Data
        @NoArgsConstructor
        public static class VoTeam {
            VoTeamInfo team;
            int numWins;
            int numDraws;
            int numLosses;
            int numGoalsFor;
            int numGoalsAgainst;
            int score;
        }

        Long groupId;
        String name;
        List<VoTeam> teamList;
    }

    @Data
    @NoArgsConstructor
    public static class VoMatchEvent {
        String matchStage;
        String matchTag;
    }

    @Data
    @NoArgsConstructor
    public static class VoMatch {
        Long matchId;
        VoTeamInfo homeTeam;
        VoTeamInfo awayTeam;
        Timestamp time;
        String status;
        String description;
        String matchLocation;
        Integer homeTeamScore;
        Integer awayTeamScore;
        Integer homeTeamPenalty;
        Integer awayTeamPenalty;
        String stage;
        String tag;
    }

    @Data
    @NoArgsConstructor
    public static class Tag {
        String tagName;
        List<VoMatch> matches;
    }

    @Data
    @NoArgsConstructor
    public static class Stage {
        String stageName;
        List<Tag> tags;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoEvent voEvent = (VoEvent) o;
        return Objects.equals(eventId, voEvent.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
