package com.example.sms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // turns on @PreAuthorize (used on controllers for "own data or admin" rules)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                // stateless JWT API: no cookies/sessions, so CSRF protection is not needed
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ---------- PUBLIC ----------
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()

                        // ---------- ADMIN ONLY (first matching rule wins) ----------
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/departments/**", "/api/courses/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/departments/**", "/api/courses/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/departments/**", "/api/courses/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/students/create").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/students/getAll", "/api/students/search").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/students/delete/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/students/*/department/*", "/api/students/*/role/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/students/*/courses/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/students/*/courses/*").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/enrollment-requests/search").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/enrollment-requests/*/approve", "/api/enrollment-requests/*/reject").hasRole("ADMIN")

                        // ---------- everything else: any logged-in user ----------
                        // ("own data or admin" rules are written with @PreAuthorize on the controllers)
                        .anyRequest().authenticated()
                )
                // validates "Authorization: Bearer <token>" and builds the roles from the token
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    /** Reads the "role" claim of the token (STUDENT / ADMIN) and turns it into ROLE_STUDENT / ROLE_ADMIN. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
