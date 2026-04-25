package com.bandmate.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "로그인 후 발급된 JWT 토큰을 입력하세요. (Bearer 접두사 없이 토큰만 입력)"
)
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("BandMate API")
                .description("""
                    밴드 팀원 모집 · 공연곡 투표 · 합주 일정 관리 플랫폼

                    **인증 방법**: 우측 상단 🔒 Authorize 버튼 → 로그인 후 받은 token 값 입력

                    **주요 흐름**
                    1. `POST /api/users/signup` → 회원가입
                    2. `POST /api/users/login` → JWT 토큰 발급
                    3. Authorize 버튼에 토큰 입력
                    4. `POST /api/bands` → 밴드 생성
                    5. `POST /api/bands/{bandId}/apply` → 지원
                    6. `POST /api/bands/{bandId}/songs/vote` → 투표
                    7. `POST /api/bands/{bandId}/rehearsals/{id}/join` → 합주 신청
                    """)
                .version("v1.0")
                .contact(new Contact()
                    .name("BandMate")
                    .url("https://github.com/Realangel0819/bandmate")))
            .servers(List.of(
                new Server().url("/").description("Current Server")
            ));
    }
}
