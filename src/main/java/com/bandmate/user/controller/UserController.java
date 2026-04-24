package com.bandmate.user.controller;

import com.bandmate.common.util.JwtUtil;
import com.bandmate.user.dto.LoginRequest;
import com.bandmate.user.dto.LoginResponse;
import com.bandmate.user.dto.SignupRequest;
import com.bandmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody @Valid SignupRequest request) {
        userService.signup(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = userService.login(request);
        String token = jwtUtil.createToken(response.getEmail(), response.getUserId());
        response.setToken(token);
        return ResponseEntity.ok(response);
    }
}