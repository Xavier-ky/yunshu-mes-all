package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.UserCompatRepository;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/user/profile")
public class ProfileCompatController {

    private final UserCompatRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public ProfileCompatController(UserCompatRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public Map<String, Object> profile() {
        String username = SysCompatHelper.currentUsername();
        return userRepo.findByUsername(username)
                .map(u -> {
                    Map<String, Object> resp = MesApiResponse.ok(u);
                    Long userId = SysCompatHelper.longVal(u.get("userId"));
                    resp.put("roleGroup", SysCompatHelper.roleGroup(userRepo.selectRolesForUser(userId)));
                    resp.put("postGroup", SysCompatHelper.postGroup(userRepo.selectPostsForUser(userId)));
                    return resp;
                })
                .orElseGet(() -> MesApiResponse.error("用户不存在"));
    }

    @OperLog(title = "个人信息", businessType = 2)
    @PutMapping
    public Map<String, Object> updateProfile(@RequestBody Map<String, Object> body) {
        String username = SysCompatHelper.currentUsername();
        return userRepo.findByUsername(username)
                .map(u -> {
                    Long userId = SysCompatHelper.longVal(u.get("userId"));
                    String phone = SysCompatHelper.str(body.get("phonenumber"));
                    if (StringUtils.hasText(phone) && userRepo.phoneExists(phone, userId)) {
                        return MesApiResponse.error("修改用户失败，手机号码已存在");
                    }
                    String email = SysCompatHelper.str(body.get("email"));
                    if (StringUtils.hasText(email) && userRepo.emailExists(email, userId)) {
                        return MesApiResponse.error("修改用户失败，邮箱账号已存在");
                    }
                    body.put("userId", userId);
                    return MesApiResponse.toAjax(userRepo.updateProfile(userId, body));
                })
                .orElseGet(() -> MesApiResponse.error("用户不存在"));
    }

    @OperLog(title = "个人信息", businessType = 2)
    @PutMapping("/updatePwd")
    public Map<String, Object> updatePwd(@RequestParam String oldPassword, @RequestParam String newPassword) {
        String username = SysCompatHelper.currentUsername();
        return userRepo.findByUsername(username)
                .map(u -> {
                    Long userId = SysCompatHelper.longVal(u.get("userId"));
                    String hash = userRepo.findPasswordHash(userId).orElse("");
                    if (!passwordEncoder.matches(oldPassword, hash)) {
                        return MesApiResponse.error("修改密码失败，旧密码错误");
                    }
                    if (passwordEncoder.matches(newPassword, hash)) {
                        return MesApiResponse.error("新密码不能与旧密码相同");
                    }
                    return MesApiResponse.toAjax(userRepo.resetPwd(userId, passwordEncoder.encode(newPassword)));
                })
                .orElseGet(() -> MesApiResponse.error("用户不存在"));
    }

    @PostMapping("/avatar")
    public Map<String, Object> avatar() {
        return MesApiResponse.error("头像上传待对接");
    }
}
