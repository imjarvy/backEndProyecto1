package com.uc.ms_security.config;

import com.uc.ms_security.authorization.DynamicAuthorizationManager;
import com.uc.ms_security.authorization.CookieBearerTokenResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final DynamicAuthorizationManager dynamicAuthorizationManager;
    private final CookieBearerTokenResolver cookieBearerTokenResolver;

    @Value("${security.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${security.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new SpaCsrfTokenRepository(cookieSecure);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(
                List.of("Content-Type", "X-XSRF-TOKEN", "Authorization")
        );
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/internal/security/permissions-validation"
                        )
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(
                                new CsrfTokenRequestAttributeHandler()
                        )
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/2fa/verify",
                                "/api/auth/oauth",
                                "/api/auth/csrf",
                                "/api/auth/logout",
                                "/api/auth/password-recovery/request",
                                "/api/auth/password-recovery/reset"
                        ).permitAll()
                        .requestMatchers("/api/auth/session").authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me/roles"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/internal/security/permissions-validation"
                        ).authenticated()
                        .anyRequest().access(dynamicAuthorizationManager)
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(cookieBearerTokenResolver)
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }
}
