package com.crative.studio_api.shared.config

import com.crative.studio_api.shared.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/auth/login", "/auth/registrar-professor").permitAll()

                    .requestMatchers(
                        HttpMethod.GET, "/alunos/**", "/professores/**",
                        "/turmas/**", "/eventos/**", "/salas/**"
                    )
                    .hasAnyRole("ADMIN", "SECRETARIA", "PROFESSOR")
                    .requestMatchers(
                        "/alunos/**", "/responsaveis/**", "/matriculas/**",
                        "/financeiro/**", "/comunicados/**", "/usuarios/**", "/salas/**"
                    ).hasAnyRole("ADMIN", "SECRETARIA")
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}