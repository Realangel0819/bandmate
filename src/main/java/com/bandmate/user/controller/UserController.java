package com.bandmate.user.controller;

import com.bandmate.common.util.JwtUtil;
import com.bandmate.user.dto.LoginRequest;
import com.bandmate.user.dto.LoginResponse;
import com.bandmate.user.dto.SignupRequest;
import com.bandmate.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "회원가입 · 로그인 · JWT 발급")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(
        summary = "회원가입",
        description = "이메일·닉네임 중복 검사 후 BCrypt 암호화로 저장합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "409", description = "이메일 또는 닉네임 중복", content = @Content(schema = @Schema(example = "{\"status\":409,\"message\":\"이미 사용 중인 이메일입니다.\"}")))
        }
    )
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody @Valid SignupRequest request) {
        userService.signup(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @Operation(
        summary = "로그인",
        description = "로그인 성공 시 JWT 토큰을 반환합니다. 이후 API 호출 시 `Authorization: Bearer {token}` 헤더에 포함하세요.",
        responses = {
            @ApiResponse(responseCode = "200", description = "로그인 성공 — token 값을 Authorize에 입력"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치", content = @Content(schema = @Schema(example = "{\"status\":401,\"message\":\"이메일 또는 비밀번호가 올바르지 않습니다.\"}")))
        }
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = userService.login(request);
        String token = jwtUtil.createToken(response.getEmail(), response.getUserId());
        response.setToken(token);
        return ResponseEntity.ok(response);
    }
}
