package com.crative.studio_api.shared.config

import com.crative.studio_api.shared.security.JwtService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class JwtConfig {

    @Bean
    fun jwtService(
        @Value("\${jwt.secret}") secret: String,
        @Value("\${jwt.expiration-ms}") expirationMs: Long
    ): JwtService {
        return JwtService(
            secret = secret,
            expirationMs = expirationMs
        )
    }
}