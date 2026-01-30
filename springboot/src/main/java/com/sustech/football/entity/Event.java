package com.sustech.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "赛事对象")
public class Event {
    @Data
    @NoArgsConstructor
    @Schema(description = "赛事队伍对象")
    public static class Team {
        public Team(Long id, String name, String logo, Integer playerCount) {
            this.id = id;
            this.name = name;
            this.logo = logo;
            this.playerCount = playerCount;
        }

        @Schema(description = "赛事队伍 ID", example = "1")
        Long id;

        @Schema(description = "赛事队伍名称", example = "致新书院 1 队")
        String name;

        @Schema(description = "赛事队伍 logo", example = "www.example.com:8085/download?filename=zhixin_logo.png")
        String logo;

        @Schema(description = "赛事队伍球员数量", example = "20")
        Integer playerCount;
    }

    @Data
    @NoArgsConstructor
    @Schema(description = "赛事小组对象")
    public static class Group {
        @Data
        @NoArgsConstructor
        @Schema(description = "赛事小组队伍对象")
        public static class Team {
            @Schema(description = "赛事小组队伍")
            Event.Team team;

            @Schema(description = "赛事小组队伍比赛场次", example = "3")
            int numWins;

            @Schema(description = "赛事小组队伍平局场次", example = "1")
            int numDraws;

            @Schema(description = "赛事小组队伍失败场次", example = "1")
            int numLosses;

            @Schema(description = "赛事小组队伍进球数", example = "5")
            int numGoalsFor;

            @Schema(description = "赛事小组队伍失球数", example = "3")
            int numGoalsAgainst;

            @Schema(description = "赛事小组队伍积分", example = "10")
            int score;
        }

        @Schema(description = "赛事小组 ID", example = "1")
        Long groupId;

        @Schema(description = "赛事小组名称", example = "2024书院杯A组")
        String name;

        @Schema(description = "赛事小组队伍列表")
        List<Team> teamList;
    }

    @Data
    @NoArgsConstructor
    @Schema(description = "赛事标签对象")
    public static class Tag {
        @Schema(description = "标签名称", example = "A组")
        String tagName;

        @Schema(description = "比赛列表")
        List<Match> matches;
    }

    @Data
    @NoArgsConstructor
    @Schema(description = "赛事阶段对象")
    public static class Stage {
        @Schema(description = "赛事阶段名称", example = "小组赛")
        String stageName;

        @Schema(description = "赛事阶段标签")
        List<Tag> tags;
    }

    @TableId(value = "event_id", type = IdType.AUTO)
    @Schema(description = "赛事 ID", example = "1")
    private Long eventId;

    @Schema(description = "赛事名称", example = "2024 年书院杯")
    private String name;

    @Schema(description = "赛事简介", example = "2024 年书院杯是一项由南科大举办的校内足球赛事")
    private String description;

    @Schema(description = "比赛人数（几人制）", example = "11")
    private Integer matchPlayerCount;

    @Schema(description = "大名单人数（每支参赛球队可报名的球员总数）", example = "23")
    private Integer rosterSize;

    @TableField(exist = false)
    @Schema(description = "赛事管理员 ID 列表")
    private List<Long> managerIdList;
    
    @TableField(exist = false)
    @Schema(description = "赛事管理员姓名列表")
    List<Event.Team> teamList; // 参加的所有球队

    @TableField(exist = false)
    @Schema(description = "赛事小组赛分组")
    List<Event.Group> groupList; // 小组赛分组，包括组别名称和小组内球队信息

    @TableField(exist = false)
    @Schema(description = "赛事所有比赛列表")
    List<Match> matchList; // 所有比赛列表，按照时间顺序排列

    @TableField(exist = false)
    @Schema(description = "赛事阶段")
    List<Stage> stageList; // 比赛阶段，包括阶段名称和二级目录“标签”，标签包括标签名称和比赛列表

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return eventId.equals(event.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }
}
