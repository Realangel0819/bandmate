package com.bandmate.user.service;

import com.bandmate.user.dto.SignupRequest;
import com.bandmate.user.entity.User;
import com.bandmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bandmate.user.dto.LoginRequest;
import com.bandmate.user.dto.LoginResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signup_Success() {
        // given (준비)
        SignupRequest request = new SignupRequest();
        request.setEmail("test@test.com");
        request.setPassword("1234");
        
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("encrypted_password");
        given(userRepository.save(any())).willReturn(User.builder().build());

        // when (실행)
        Long userId = userService.signup(request);

        // then (검증)
        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("중복 이메일 회원가입 실패")
    void signup_DuplicateEmail_Fail() {
        SignupRequest request = new SignupRequest();
        request.setEmail("dup@test.com");
        request.setPassword("1234");

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(User.builder().build()));

        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 존재하는 이메일입니다.");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("1234");

        User user = User.builder()
                .email("test@test.com")
                .password("encoded")
                .nickname("tester")
                .build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("1234", "encoded")).willReturn(true);

        LoginResponse response = userService.login(request);

        assertThat(response.getEmail()).isEqualTo("test@test.com");
        assertThat(response.getNickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("잘못된 비밀번호 로그인 실패")
    void login_WrongPassword_Fail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("wrong");

        User user = User.builder()
                .email("test@test.com")
                .password("encoded")
                .nickname("tester")
                .build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}