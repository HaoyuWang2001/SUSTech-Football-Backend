package com.sustech.football.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sustech.football.entity.*;
import com.sustech.football.exception.*;
import com.sustech.football.service.*;
import com.sustech.football.utils.WXBizDataCrypt;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@CrossOrigin
@RequestMapping("/user")
@Tag(name = "User Controller", description = "用户管理接口")
public class UserController {

    private UserService userService;
    private String appid = "wxca12f9a07b0c63e2";
    private String appsecret = "1d3743bc2b7b109493ba284ccbaa2420";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final EventManagerService eventManagerService;

    private final EventTeamRequestService eventTeamRequestService;

    private final TeamManagerService teamManagerService;

    private final TeamPlayerRequestService teamPlayerRequestService;

    @Autowired
    public UserController(UserService userService, EventManagerService eventManagerService, EventTeamRequestService eventTeamRequestService, TeamManagerService teamManagerService, TeamPlayerRequestService teamPlayerRequestService) {
        this.userService = userService;
        this.eventManagerService = eventManagerService;
        this.eventTeamRequestService = eventTeamRequestService;
        this.teamManagerService = teamManagerService;
        this.teamPlayerRequestService = teamPlayerRequestService;
    }

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/wxLogin")
    @Operation(summary = "微信登录", description = "使用微信小程序登录")
    @Parameter(name = "code", description = "微信登录凭证", required = true)
    public ResponseEntity<Map> wxLogin(String code) {
//        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appid + "&secret=" + appsecret + "&js_code=" + code + "&grant_type=authorization_code";
//        String response = restTemplate.getForObject(url, String.class);
//        return ResponseEntity.ok().body(response);

        // 1. 校验入参
        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("登录凭证code不能为空");
        }

        // 2. 拼接微信接口地址，获取session_key和openid
        String wxUrl = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appid, appsecret, code
        );

        // 3. 调用微信接口并解析返回结果（用Map接收JSON，避免创建冗余实体类）
        String wxResponseStr;
        try {
            wxResponseStr = restTemplate.getForObject(wxUrl, String.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "调用微信登录接口失败：" + e.getMessage());
        }

        Map<String, String> wxResponse;
        try {
            wxResponse = objectMapper.readValue(wxResponseStr, Map.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("解析微信登录JSON失败：原始响应=[%s]，错误原因=[%s]", wxResponseStr, e.getMessage()),
                    e
            );
        }

        // 4. 校验微信接口返回结果（防止返回错误信息，如code无效）
        if (wxResponse == null || wxResponse.containsKey("errcode")) {
            String errMsg = wxResponse != null ? wxResponse.get("errmsg") : "微信接口返回空";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "微信登录失败：" + errMsg);
        }

        // 5. 提取session_key和openid
        String sessionKey = wxResponse.get("session_key");
        String openid = wxResponse.get("openid");
        if (sessionKey == null || openid == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "解析微信登录信息失败");
        }

        // 6. 查询/新增/更新用户信息
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("openid", openid); // 用eq精准匹配，like会导致查询不准确
        User user = userService.getOne(queryWrapper);

        if (user == null) {
            // 6.1 新增用户
            user = new User();
            user.setOpenid(openid);
            user.setSessionKey(sessionKey); // 假设User类有setter方法
            if (!userService.save(user)) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "注册失败");
            }
        } else {
            // 6.2 更新用户session_key
            UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("openid", openid)
                    .set("session_key", sessionKey);
            if (!userService.update(updateWrapper)) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "更新用户session_key失败");
            }
        }

        // 7. 封装返回结果（包含微信响应+用户信息）
        Map<String, Object> result = new HashMap<>();
        result.put("openid", openid); // 微信唯一标识
        result.put("session_key", sessionKey); // 微信会话密钥
        result.put("userId", user.getUserId());
        result.put("nickName", user.getNickName());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    @Operation(summary = "登录", description = "使用 openid 和 session_key 登录")
    @Parameter(name = "openid", description = "用户 openid", required = true)
    public ResponseEntity<User> login(String openid, String session_key) {
        if (openid == null || session_key == null) {
            throw new BadRequestException("openid和session_key不能为空");
        }
        System.out.println(openid + "\n" + session_key);
//        String access_token = "ACCESS_TOKEN";
        // signature = hmac_sha256(session_key, "")
//        String url = "GET https://api.weixin.qq.com/wxa/checksession?access_token=ACCESS_TOKEN&signature=SIGNATURE&openid=OPENID&sig_method=hmac_sha256";
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("openid", openid);
        User user = userService.getOne(queryWrapper);
        if (user == null) {
            user = new User(openid, session_key);
            if (!userService.save(user)) {
                throw new InternalServerErrorException("注册失败");
            }
        } else {
            UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("openid", openid).set("session_key", session_key);
            if (!userService.update(updateWrapper)) {
                throw new InternalServerErrorException("???");
            }
        }

        return ResponseEntity.ok().body(user);
    }

    @PostMapping("/get")
    @Operation(summary = "获取用户", description = "根据用户 ID 获取用户信息")
    @Parameter(name = "userId", description = "用户 ID", required = true)
    public User getUser(Long userId) {
        if (userId == null) {
            throw new BadRequestException("参数错误");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        user.setOpenid(null);
        user.setSessionKey(null);
        return user;
    }

    @PostMapping("/update")
    @Operation(summary = "更新用户", description = "更新用户信息")
    @Parameter(name = "userId", description = "用户 ID", required = true)
    public void update(Long userId, @RequestParam(required = false) String avatarUrl, @RequestParam(required = false) String nickName) {
        if (userId == null || avatarUrl == null || nickName == null) {
            throw new BadRequestException("参数错误");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        if (!avatarUrl.isEmpty()) {
            user.setAvatarUrl(avatarUrl);
        }
        if (!nickName.isEmpty()) {
            user.setNickName(nickName);
        }
        if (!userService.updateById(user)) {
            throw new InternalServerErrorException("更新失败");
        }
    }

    @GetMapping("/getData")
    @Operation(summary = "获取用户数据", description = "获取用户数据")
    @Parameters({
            @Parameter(name = "encryptedData", description = "加密数据", required = true),
            @Parameter(name = "iv", description = "加密算法的初始向量", required = true),
            @Parameter(name = "userId", description = "用户 ID", required = true)
    })
    public String getData(String encryptedData, String iv, String userId) {
        User user = userService.getById(Long.parseLong(userId));
        if (user == null) {
            throw new UnauthorizedAccessException("用户不存在");
        }
        String sessionKey = user.getSessionKey();
        WXBizDataCrypt crypt = new WXBizDataCrypt(appid, sessionKey);
        System.out.println(encryptedData);
        System.out.println(iv);
        System.out.println(appid);
        System.out.println(sessionKey);
        String result = "";
        try {
            JSONObject decryptedData = crypt.decryptData(encryptedData, iv);
            result = decryptedData.toString();
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalServerErrorException("解密失败");
        }
        return result;
    }

    @GetMapping("/getUserManageMatch")
    @Operation(summary = "获取用户管理的比赛", description = "根据用户 ID，获取用户管理的比赛")
    @Parameter(name = "userId", description = "用户 ID", required = true)
    public List<Match> getUserManageMatch(Long userId) {
        if (userId == null) {
            throw new BadRequestException("用户ID不能为空");
        }
        if (userService.getById(userId) == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        return userService.getUserManageMatches(userId);
    }

    @GetMapping("/getUserManageEvent")
    @Operation(summary = "获取用户管理的赛事", description = "根据用户 ID，获取用户管理的赛事")
    @Parameter(name = "userId", description = "用户 ID", required = true)
    public List<com.sustech.football.entity.Event> getUserManageEvent(Long userId) {
        if (userId == null) {
            throw new BadRequestException("用户ID不能为空");
        }
        return userService.getUserManageEvents(userId);
    }

    @GetMapping("/getUserManageTeam")
    @Operation(summary = "获取用户管理的球队", description = "根据用户 ID，获取用户管理的球队")
    @Parameter(name = "userId", description = "用户 ID", required = true)
    public List<com.sustech.football.entity.Team> getUserManageTeam(Long userId) {
        if (userId == null) {
            throw new BadRequestException("用户ID不能为空");
        }
        return userService.getUserManageTeams(userId);
    }

    @GetMapping("/getPlayerId")
    @Operation(summary = "获取球员 ID", description = "根据用户 ID，获取球员 ID")
    @Parameter(name = "userId", description = "用户 ID", required = true)
    public Long getPlayerId(Long userId) {
        if (userId == null) {
            throw new BadRequestException("用户ID不能为空");
        }
        if (userService.getById(userId) == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        return userService.getPlayerId(userId);
    }

    @GetMapping("/getCoachId")
    @Operation(summary = "获取教练 ID", description = "根据用户 ID，获取教练 ID")
    @Parameter(name = "userId", description = "用户 ID", required = true)
    public Long getCoachId(Long userId) {
        if (userId == null) {
            throw new BadRequestException("用户ID不能为空");
        }
        if (userService.getById(userId) == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        return userService.getCoachId(userId);
    }

    @GetMapping("/getRefereeId")
    @Operation(summary = "获取裁判 ID", description = "根据用户 ID，获取裁判 ID")
    @Parameter(name = "userId", description = "用户 ID", required = true)
    public Long getRefereeId(Long userId) {
        if (userId == null) {
            throw new BadRequestException("用户ID不能为空");
        }
        if (userService.getById(userId) == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        return userService.getRefereeId(userId);
    }

    @PostMapping("/event/team/readReplies")
    @Operation(summary = "标记赛事邀请球队回复为已阅", description = "提供用户 ID，标记用户管理的赛事邀请回复为已阅")
    @Parameter(name = "userId", description = "用户 ID", required = true)
    public void readEventTeamReplies(Long userId) {
        if (userId == null) {
            throw new BadRequestException("用户ID不能为空");
        }
        if (userService.getById(userId) == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        List<Long> eventIds = eventManagerService.list(
                new QueryWrapper<EventManager>()
                        .eq("user_id", userId)
        ).stream().map(EventManager::getEventId).toList();

        if (eventIds.isEmpty()) {
            throw new ResourceNotFoundException("用户未管理任何赛事");
        }

        List<EventTeamRequest> eventTeamRequests = eventTeamRequestService.list(
                new QueryWrapper<EventTeamRequest>()
                        .in("event_id", eventIds)
                        .eq("has_read", false)
        );
        eventTeamRequests.forEach(request -> {
            request.setHasRead(true);
            eventTeamRequestService.updateByMultiId(request);
        });
    }

    @PostMapping("/team/player/readReplies")
    @Operation(summary = "标记球队邀请球员回复为已阅", description = "提供用户 ID，标记用户管理的球队邀请回复为已阅")
    @Parameter(name = "userId", description = "用户 ID", required = true)
    public void readTeamPlayerReplies(Long userId) {
        if (userId == null) {
            throw new BadRequestException("用户ID不能为空");
        }
        if (userService.getById(userId) == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        List<Long> teamIds = teamManagerService.list(
                new QueryWrapper<TeamManager>()
                        .eq("user_id", userId)
        ).stream().map(TeamManager::getTeamId).toList();

        if (teamIds.isEmpty()) {
            throw new ResourceNotFoundException("用户未管理任何球队");
        }

        List<TeamPlayerRequest> teamPlayerRequests = teamPlayerRequestService.list(
                new QueryWrapper<TeamPlayerRequest>()
                        .in("team_id", teamIds)
                        .eq("has_read", false)
        );
        teamPlayerRequests.forEach(request -> {
            request.setHasRead(true);
            teamPlayerRequestService.updateByMultiId(request);
        });
    }

    @GetMapping("/getAllRoleUsers")
    @Operation(summary = "获取所有有职责的用户", description = "获取所有有职责的用户")
    public List<UserRole> getAllRoleUsers() {
        return userService.getAllRoleUsers();
    }

    @GetMapping("getRoleUserById")
    @Operation(summary = "获取用户职责", description = "根据用户 ID，获取用户职责")
    @Parameter(name = "userId", description = "用户 ID", required = true)
    public UserRole getRoleUserById(Long userId) {
        if (userId == null) {
            throw new BadRequestException("用户ID不能为空");
        }
        return userService.getRoleUserById(userId);
    }
}
