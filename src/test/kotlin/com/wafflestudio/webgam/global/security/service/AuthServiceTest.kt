package com.wafflestudio.webgam.global.security.service

import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.security.dto.AuthDto.LoginRequest
import com.wafflestudio.webgam.global.security.dto.AuthDto.SignupRequest
import com.wafflestudio.webgam.global.security.exception.DuplicateUserIdentifierException
import com.wafflestudio.webgam.global.security.exception.InvalidJwtException
import com.wafflestudio.webgam.global.security.exception.LoginFailedException
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

@Tag("Unit-Test")
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest : DescribeSpec() {

    companion object {
        private val userRepository = mockk<UserRepository>()
        private val passwordEncoder = mockk<PasswordEncoder>()
        private val jwtProvider = mockk<JwtProvider>()
        private val authService = AuthService(userRepository, passwordEncoder, jwtProvider)
        private val dummyUser = User("", "", "", "")
        private val deletedDummyUser = User("", "", "", "")
    }

    init {
        this.describe("login 호출될 때") {
            val request = LoginRequest("", "")

            context("성공적이면") {
                every { userRepository.findByUserId(any()) } returns dummyUser
                every { passwordEncoder.matches(any(), any()) } returns true
                every { jwtProvider.generateToken(any(), any()) } returns ("access_token" to "refresh_token")

                val (response, refreshToken) = withContext(Dispatchers.IO) {
                    authService.login(request)
                }

                it("유저 정보가 반환된다.") {
                    assertThat(response).hasFieldOrProperty("user")
                    assertThat(response.user).hasFieldOrProperty("userId")
                    assertThat(response.user).hasFieldOrProperty("username")
                    assertThat(response.user).hasFieldOrProperty("email")
                }

                it("JWT Access 토큰이 반환된다.") {
                    assertThat(response).hasFieldOrProperty("accessToken")
                    assertThat(response.accessToken).isEqualTo("access_token")
                }

                it("JWT Refresh 토큰이 반환된다.") {
                    assertThat(refreshToken).isEqualTo("refresh_token")
                }
            }

            context("회원가입이 되어 있지 않으면") {
                every { userRepository.findByUserId(any()) } returns null

                it("LoginFailedException 예외를 던진다") {
                    assertThrows<LoginFailedException> { authService.login(request) }
                }
            }

            context("회원정보가 삭제되었으면") {
                every { userRepository.findByUserId(any()) } returns deletedDummyUser
                deletedDummyUser.delete()

                it("LoginFailedException 예외를 던진다") {
                    assertThrows<LoginFailedException> { authService.login(request) }
                }
            }

            context("비밀번호가 맞지 않으면") {
                every { passwordEncoder.matches(any(), any()) } returns false

                it("LoginFailedException 예외를 던진다") {
                    assertThrows<LoginFailedException> { authService.login(request) }
                }
            }
        }

        this.describe("signup 호출될 때") {
            val request = SignupRequest("", "", "", "")

            context("성공적이면") {
                every { userRepository.existsByUserIdOrEmail(any(), any()) } returns false
                every { passwordEncoder.encode(any()) } returns "password"
                every { jwtProvider.generateToken(any(), any()) } returns ("access_token" to "refresh_token")
                every { userRepository.save(any()) } returns dummyUser

                val (response, refreshToken) = withContext(Dispatchers.IO) {
                    authService.signup(request)
                }

                it("유저 정보가 반환된다.") {
                    assertThat(response).hasFieldOrProperty("user")
                    assertThat(response.user).hasFieldOrProperty("userId")
                    assertThat(response.user).hasFieldOrProperty("username")
                    assertThat(response.user).hasFieldOrProperty("email")
                }

                it("JWT Access 토큰이 반환된다.") {
                    assertThat(response).hasFieldOrProperty("accessToken")
                    assertThat(response.accessToken).isEqualTo("access_token")
                }

                it("JWT Refresh 토큰이 반환된다.") {
                    assertThat(refreshToken).isEqualTo("refresh_token")
                }
            }

            context("동일한 아이디나 이메일이 존재하면") {
                every { userRepository.existsByUserIdOrEmail(any(), any()) } returns true

                it("DuplicateUserIdentifierException 예외를 던진다") {
                    assertThrows<DuplicateUserIdentifierException> { authService.signup(request) }
                }
            }
        }

        this.describe("refreshToken 호출될 때") {
            context("성공적이면") {
                every { jwtProvider.generateToken(any()) } returns ("access_token" to "refresh_token")

                val (response, refreshToken) = withContext(Dispatchers.IO) {
                    authService.refreshToken("token")
                }

                it("JWT Access 토큰이 반환된다.") {
                    assertThat(response).hasFieldOrProperty("accessToken")
                    assertThat(response.accessToken).isEqualTo("access_token")
                }

                it("JWT Refresh 토큰이 반환된다.") {
                    assertThat(refreshToken).isEqualTo("refresh_token")
                }
            }

            context("유효하지 않은 토큰인 경우") {
                every { jwtProvider.generateToken(any()) } throws InvalidJwtException("")

                it("InvalidJwtException 예외를 던진다") {
                    assertThrows<InvalidJwtException> { authService.refreshToken("token") }
                }
            }
        }
    }
}