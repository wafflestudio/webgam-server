package com.wafflestudio.webgam.global.security.service

import com.wafflestudio.webgam.domain.user.dto.UserDto
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.security.dto.AuthDto.*
import com.wafflestudio.webgam.global.security.dto.JwtDto
import com.wafflestudio.webgam.global.security.exception.DuplicateUserIdentifierException
import com.wafflestudio.webgam.global.security.exception.LoginFailedException
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import com.wafflestudio.webgam.global.security.model.WebgamRoles.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${webgam.admin-password}")
    private val adminPassword = ""

    @Value("\${webgam.dev-password}")
    private val developerPassword = ""

    fun login(loginRequest: LoginRequest): Pair<Response, String> {
        val user = userRepository.findByUserId(loginRequest.userId) ?: throw LoginFailedException()
        if (user.isDeleted) throw LoginFailedException()

        if (!passwordEncoder.matches(loginRequest.password, user.password)) throw LoginFailedException()

        val (accessToken, refreshToken) = jwtProvider.generateToken(user.userId, USER)

        return Response(UserDto.SimpleResponse(user), "login success", accessToken) to refreshToken
    }
    
    @Transactional
    fun signup(signupRequest: SignupRequest): Pair<Response, String> {
        if (userRepository.existsByUserIdOrEmail(signupRequest.userId!!, signupRequest.email!!))
            throw DuplicateUserIdentifierException()

        val user = User(signupRequest, passwordEncoder.encode(signupRequest.password))
        val (accessToken, refreshToken) = jwtProvider.generateToken(user.userId, USER)

        SecurityContextHolder.getContext().authentication = WebgamAuthenticationToken(UserPrincipal(user), accessToken)

        userRepository.save(user)
        return Response(UserDto.SimpleResponse(user), "login success", accessToken) to refreshToken
    }
    
    fun refreshToken(token: String): Pair<JwtDto.AccessToken, String> {
        val (accessToken, refreshToken) = jwtProvider.generateToken(token)

        return JwtDto.AccessToken(accessToken) to refreshToken
    }

    fun adminLogin(loginRequest: LoginRequest): JwtDto.AccessToken {
        return when (loginRequest.userId) {
            "admin" -> {
                if (loginRequest.password != adminPassword) {
                    logger.warn("ADMIN 계정으로 로그인 시도가 있었습니다.")
                    throw LoginFailedException()
                }
                JwtDto.AccessToken(jwtProvider.generateToken("admin", ADMIN).first)
            }
            "dev" -> {
                if (loginRequest.password != developerPassword) {
                    logger.warn("DEV 계정으로 로그인 시도가 있었습니다.")
                    throw LoginFailedException()
                }
                JwtDto.AccessToken(jwtProvider.generateToken("dev", DEV).first)
            }
            else -> throw LoginFailedException()
        }
    }
}