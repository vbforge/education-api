package com.vbforge.educationapi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize on service/controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // disable CSRF for REST API calls from Postman / HTTP client
                // we will re-enable it for Thymeleaf forms in Phase 5
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth

                        // ── Public ──────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/v1/students/register").permitAll()
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()   // dev only

                        // ── Courses: read = anyone logged in ────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/courses/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/modules/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/assignments/**").authenticated()

                        // ── Courses: write = INSTRUCTOR or ADMIN ────────
                        .requestMatchers(HttpMethod.POST,   "/api/v1/courses/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/courses/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST,   "/api/v1/modules/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/modules/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/modules/**").hasAnyRole("INSTRUCTOR", "ADMIN")

                        .requestMatchers(HttpMethod.POST,   "/api/v1/assignments/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/assignments/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/assignments/**").hasAnyRole("INSTRUCTOR", "ADMIN")

                        // ── Enrollments ──────────────────────────────────
                        .requestMatchers("/api/v1/enrollments/**").authenticated()

                        // ── Submissions ──────────────────────────────────
                        .requestMatchers(HttpMethod.POST,  "/api/v1/submissions/**").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/submissions/*/grade").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET,   "/api/v1/submissions/**").authenticated()

                        // ── Students: admin only for list/delete ─────────
                        .requestMatchers(HttpMethod.GET,    "/api/v1/students").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/students/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/students/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/students/**").authenticated()

                        // file downloads — authenticated users only
                        .requestMatchers(HttpMethod.GET, "/api/v1/files/**").authenticated()

                        // ── Everything else requires login ───────────────
                        .anyRequest().authenticated()
                )

                // ── Form login (for Thymeleaf UI) ───────────────────
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")        // Spring handles POST /login
                        //.successHandler(successHandler())    // redirect by role after login
                        .defaultSuccessUrl("/api/v1/courses", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                // ── Logout ───────────────────────────────────────────
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // ── H2 console fix (dev only) ─────────────────────
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin      = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isInstructor = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_INSTRUCTOR"));

            if (isAdmin) {
                response.sendRedirect("/admin/dashboard");
            } else if (isInstructor) {
                response.sendRedirect("/instructor/dashboard");
            } else {
                response.sendRedirect("/student/dashboard");
            }
        };
    }

}




















