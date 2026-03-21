package com.sustech.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "球员对象")
public class Player {
    @TableId(type = IdType.AUTO)
    @Schema(description = "球员 ID", example = "1")
    private Long playerId;

    @Schema(description = "球员姓名", example = "李四")
    private String name;

    @Schema(description = "球员头像链接", example = "www.example.com:8085/download?filename=lisi_photo.png")
    private String photoUrl;

    @Schema(description = "球员生日", example = "2000-01-01")
    private Date birthDate;

    @Schema(description = "球员身高", example = "180")
    private Integer height;

    @Schema(description = "球员体重", example = "70")
    private Integer weight;

    @Schema(description = "球员位置", example = "前锋")
    private String position;

    @Schema(description = "学工号", example = "12010101")
    private String identity;

    @Schema(description = "所属书院", example = "树德书院")
    private String shuYuan;

    @Schema(description = "所属院系", example = "计算机科学与工程系")
    private String college;

    @Schema(description = "入学年份", example = "2020")
    private Integer admissionYear;

    @Schema(description = "球员简介", example = "我是李四，我是一名前锋")
    private String bio;

    @Schema(description = "球员用户 ID", example = "1")
    private Long userId;

    @TableField(exist = false)
    @Schema(description = "球员所属球队列表")
    private List<Team> teamList;

    @TableField(exist = false)
    @Schema(description = "球员号码", example = "10")
    private Integer number;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(playerId, player.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }
}
