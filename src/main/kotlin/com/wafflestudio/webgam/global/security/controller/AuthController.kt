package com.wafflestudio.webgam.global.security.controller

import com.wafflestudio.webgam.global.security.dto.AuthDto
import com.wafflestudio.webgam.global.security.dto.AuthDto.LoginRequest
import com.wafflestudio.webgam.global.security.dto.AuthDto.SignupRequest
import com.wafflestudio.webgam.global.security.dto.JwtDto
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import com.wafflestudio.webgam.global.security.service.AuthService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.net.URLEncoder

@RestController
class AuthController(
    private val authService: AuthService
) {
    @Value("\${spring.profiles.active}")
    private val activeProfile = ""

    @PostMapping("/login")
    fun login(@RequestBody @Valid loginRequest: LoginRequest): ResponseEntity<AuthDto.Response> {
        val (response, refreshToken) = authService.login(loginRequest)

        val cookie = ResponseCookie.from("refreshToken", URLEncoder.encode(refreshToken, Charsets.UTF_8))
            .maxAge(JwtProvider.refreshTokenValidTime)
            .path("/")
            .secure(activeProfile == "prod")
            .sameSite("None")
            .httpOnly(true)
            .build().toString()

        return ResponseEntity.ok().header("Set-cookie", cookie).body(response)
    }

    @PostMapping("/signup")
    fun signup(@RequestBody @Valid signupRequest: SignupRequest): ResponseEntity<AuthDto.Response> {
        val (response, refreshToken) = authService.signup(signupRequest)

        val cookie = ResponseCookie.from("refreshToken", URLEncoder.encode(refreshToken, Charsets.UTF_8))
            .maxAge(JwtProvider.refreshTokenValidTime)
            .path("/")
            .secure(activeProfile == "prod")
            .sameSite("None")
            .httpOnly(true)
            .build().toString()

        return ResponseEntity.ok().header("Set-cookie", cookie).body(response)
    }

    @PostMapping("/refresh")
    fun refresh(@CookieValue("refresh_token") token: String): ResponseEntity<JwtDto.AccessToken> {
        val (response, refreshToken) = authService.refreshToken(URLDecoder.decode(token, Charsets.UTF_8))

        val cookie = ResponseCookie.from("refreshToken", URLEncoder.encode(refreshToken, Charsets.UTF_8))
            .maxAge(JwtProvider.refreshTokenValidTime)
            .path("/")
            .secure(activeProfile == "prod")
            .sameSite("None")
            .httpOnly(true)
            .build().toString()

        return ResponseEntity.ok().header("Set-cookie", cookie).body(response)
    }

    @GetMapping("/auth-ping")
    fun authPingTest(): ResponseEntity<String> {
        return ResponseEntity.ok("auth-pong")
    }

    @PostMapping("/login/admin")
    fun adminLogin(@RequestBody @Valid loginRequest: LoginRequest): ResponseEntity<JwtDto.AccessToken> {
        return ResponseEntity.ok(authService.adminLogin(loginRequest))
    }
}