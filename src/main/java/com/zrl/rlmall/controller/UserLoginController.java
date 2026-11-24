package com.zrl.rlmall.controller;

import com.zrl.rlmall.dto.LoginFormDTO;
import com.zrl.rlmall.dto.Result;
import com.zrl.rlmall.dto.UserDTO;
import com.zrl.rlmall.entity.UserInfo;
import com.zrl.rlmall.service.UserInfoService;
import com.zrl.rlmall.service.UserService;
import com.zrl.rlmall.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "用户与登录")
@RestController
@RequestMapping("/user")
public class UserLoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserInfoService userInfoService;

    /**
     *
     * 发送手机验证码
     * @param phone
     * @param session
     * @return
     */
    @Operation(summary = "获取手机验证码")
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session){

        return userService.sendCode(phone,session);
    }

    @Operation(summary = "使用手机号和验证码登录")
    @PostMapping("login")
    public Result login(@RequestParam("Phone")LoginFormDTO loginForm, HttpSession session){

        return userService.login(loginForm,session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @Operation(summary = "退出登录（未实现）")
    @PostMapping("/logout")
    public Result logout(){
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("me")
    public Result me(){
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @Operation(summary = "按用户 ID 查询用户详情")
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }
}
