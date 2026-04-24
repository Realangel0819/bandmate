package com.bandmate.user.service;

import com.bandmate.common.exception.DuplicateException;
import com.bandmate.common.exception.InvalidRequestException;
import com.bandmate.user.dto.LoginRequest;
import com.bandmate.user.dto.LoginResponse;
import com.bandmate.user.dto.SignupRequest;
import com.bandmate.user.entity.User;
import com.bandmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Long signup(SignupRequest request) {
        // 이메일 중복 확인
        userRepository.findByEmail(request.getEmail())
                .ifPresent(u -> { throw new DuplicateException("이미 존재하는 이메일입니다."); });

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // 암호화
                .nickname(request.getNickname())
                .build();

        return userRepository.save(user).getId();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidRequestException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidRequestException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return new LoginResponse(null, user.getId(), user.getEmail(), user.getNickname());
    }
}