package com.sustech.football.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sustech.football.entity.*;
import com.sustech.football.exception.BadRequestException;
import com.sustech.football.mapper.*;
import com.sustech.football.service.UserFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class UserFavoriteServiceImpl implements UserFavoriteService {
    @Autowired
    private FavoriteUserMapper favoriteUserMapper;

    @Autowired
    private FavoriteEventMapper favoriteEventMapper;

    @Autowired
    private FavoriteTeamMapper favoriteTeamMapper;

    @Autowired
    private FavoriteMatchMapper favoriteMatchMapper;
    @Autowired
    private MatchMapper matchMapper;
    @Autowired
    private TeamMapper teamMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EventMapper eventMapper;
    @Autowired
    private PlayerMapper playerMapper;
    @Autowired
    private EventMatchMapper eventMatchMapper;

    @Override
    public boolean favoriteTeam(Long userId, Long teamId) {
        if (teamMapper.selectById(teamId) != null) {
            if (favoriteTeamMapper.selectByMultiId(new FavoriteTeam(userId, teamId)) != null) {
                throw new BadRequestException("已收藏过的队伍");
            } else {
                favoriteTeamMapper.insert(new FavoriteTeam(userId, teamId));
                return true;
            }
        } else {
            throw new BadRequestException("要收藏的队伍不存在");
        }
    }

    @Override
    public boolean unfavoriteTeam(Long userId, Long teamId) {
        if (favoriteTeamMapper.selectByMultiId(new FavoriteTeam(userId, teamId)) != null) {
            favoriteTeamMapper.deleteByMultiId(new FavoriteTeam(userId, teamId));
            return true;
        } else {
            throw new BadRequestException("未收藏或不存在的队伍");
        }
    }

    @Override
    public List<Team> getFavoriteTeams(Long userId) {
        return favoriteTeamMapper.selectFavoriteWithUser(userId).stream().map(FavoriteTeam::getTeam).sorted(Comparator.comparing(Team::getTeamId)).toList();
    }

    @Override
    public boolean favoriteUser(Long userId, Long favoriteId) {
        if (Objects.equals(userId, favoriteId)) {
            throw new BadRequestException("不能收藏自己");
        }
        if (userMapper.selectById(favoriteId) != null) {
            if (favoriteUserMapper.selectByMultiId(new FavoriteUser(userId, favoriteId)) != null) {
                throw new BadRequestException("已收藏过的用户");
            } else {
                favoriteUserMapper.insert(new FavoriteUser(userId, favoriteId));
                return true;
            }
        } else {
            throw new BadRequestException("要收藏的用户不存在");
        }
    }

    @Override
    public boolean unfavoriteUser(Long userId, Long favoriteId) {
        if (favoriteUserMapper.selectByMultiId(new FavoriteUser(userId, favoriteId)) != null) {
            favoriteUserMapper.deleteByMultiId(new FavoriteUser(userId, favoriteId));
            return true;
        } else {
            throw new BadRequestException("未收藏或不存在的用户");
        }
    }

    @Override
    public List<User> getFavoriteUsers(Long userId) {
        List<FavoriteUser> favoriteUsers = favoriteUserMapper.selectFavoriteWithUser(userId);
        List<User> users = favoriteUsers.stream().map(FavoriteUser::getFavoriteUser).sorted(Comparator.comparing(User::getUserId)).toList();
        for (User user : users) {
            user.setSessionKey(null);
            user.setOpenid(null);
        }
//        for (User user : users) {
//            QueryWrapper<Player> queryWrapper = new QueryWrapper<>();
//            queryWrapper.in("user_id", user.getUserId());
//            Player player = playerMapper.selectOne(queryWrapper);
//            if (player == null) {
//                continue;
//            }
//            user.setAvatarUrl(player.getPhotoUrl());
//            user.setNickName(player.getName());
//        }
        return users;
    }

    @Override
    public boolean favoriteMatch(Long userId, Long matchId) {
        if (matchMapper.selectById(matchId) != null) {
            if (favoriteMatchMapper.selectByMultiId(new FavoriteMatch(userId, matchId)) != null) {
                throw new BadRequestException("已收藏过的比赛");
            } else {
                favoriteMatchMapper.insert(new FavoriteMatch(userId, matchId));
                return true;
            }
        } else {
            throw new BadRequestException("要收藏的比赛不存在");
        }
    }

    @Override
    public boolean unfavoriteMatch(Long userId, Long matchId) {
        if (favoriteMatchMapper.selectByMultiId(new FavoriteMatch(userId, matchId)) != null) {
            favoriteMatchMapper.deleteByMultiId(new FavoriteMatch(userId, matchId));
            return true;
        } else {
            throw new BadRequestException("未收藏或不存在的比赛");
        }
    }

    @Override
    public List<Match> getFavoriteMatches(Long userId) {
        List<Match> matches = favoriteMatchMapper.selectFavoriteWithUser(userId).stream().map(FavoriteMatch::getMatch).sorted(Comparator.comparing(Match::getTime)).toList();
        for (Match match : matches) {
            match.setHomeTeam(teamMapper.selectById(match.getHomeTeamId()));
            match.setAwayTeam(teamMapper.selectById(match.getAwayTeamId()));
            EventMatch eventMatch = eventMatchMapper.selectEventMatchWithMatch(match.getMatchId());
            match.setMatchEvent(new MatchEvent(eventMatch));
        }
        return matches;
    }

    @Override
    public boolean favoriteEvent(Long userId, Long eventId) {
        if (eventMapper.selectById(eventId) != null) {
            if (favoriteEventMapper.selectByMultiId(new FavoriteEvent(userId, eventId)) != null) {
                throw new BadRequestException("已收藏过的赛事");
            } else {
                favoriteEventMapper.insert(new FavoriteEvent(userId, eventId));
                return true;
            }
        } else {
            throw new BadRequestException("要收藏的赛事不存在");
        }
    }

    @Override
    public boolean unfavoriteEvent(Long userId, Long eventId) {
        if (favoriteEventMapper.selectByMultiId(new FavoriteEvent(userId, eventId)) != null) {
            favoriteEventMapper.deleteByMultiId(new FavoriteEvent(userId, eventId));
            return true;
        } else {
            throw new BadRequestException("未收藏或不存在的赛事");
        }
    }

    @Override
    public List<Event> getFavoriteEvents(Long userId) {
        return favoriteEventMapper.selectFavoriteWithUser(userId).stream().map(FavoriteEvent::getEvent).sorted(Comparator.comparing(Event::getEventId)).toList();
    }

    @Override
    public boolean isFavoriteTeam(Long userId, Long teamId) {
        return favoriteTeamMapper.selectByMultiId(new FavoriteTeam(userId, teamId)) != null;
    }

    @Override
    public boolean isFavoriteUser(Long userId, Long favoriteId) {
        return favoriteUserMapper.selectByMultiId(new FavoriteUser(userId, favoriteId)) != null;
    }

    @Override
    public boolean isFavoriteMatch(Long userId, Long matchId) {
        return favoriteMatchMapper.selectByMultiId(new FavoriteMatch(userId, matchId)) != null;
    }

    @Override
    public boolean isFavoriteEvent(Long userId, Long eventId) {
        return favoriteEventMapper.selectByMultiId(new FavoriteEvent(userId, eventId)) != null;
    }
}
