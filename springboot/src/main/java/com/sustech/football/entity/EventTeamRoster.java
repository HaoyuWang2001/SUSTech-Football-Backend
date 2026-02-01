package com.sustech.football.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("event_team_roster")
@Schema(description = "赛事球队大名单对象")
public class EventTeamRoster {
    @Schema(description = "赛事 ID", example = "1")
    private Long eventId;

    @Schema(description = "球队 ID", example = "1")
    private Long teamId;

    @Schema(description = "球员 ID", example = "1")
    private Long playerId;

    @Schema(description = "球员号码", example = "10")
    private Integer number;

    public EventTeamRoster(Long eventId, Long teamId, Long playerId) {
        this.eventId = eventId;
        this.teamId = teamId;
        this.playerId = playerId;
    }
}
