package com.wafflestudio.webgam.global.security.jwt

import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.security.exception.InvalidJwtException
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import com.wafflestudio.webgam.global.security.model.WebgamRoles.USER
import com.wafflestudio.webgam.global.security.service.UserPrincipalDetailsService
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.inspectors.forAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@Tag("Unit-Test")
@DisplayName("JWT Provider 단위 테스트")
class JwtProviderTest : DescribeSpec() {

    companion object {
        private val userRepository = mockk<UserRepository>()
        private val userPrincipalDetailService = UserPrincipalDetailsService(userRepository)
        private val signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256)
    }

    init {
        // Kotlin Reflection for private member properties and functions
        val kClass = JwtProvider::class
        val jwtProvider = kClass.constructors.first().call(userPrincipalDetailService)
        val signingKeyProperty = kClass.memberProperties.find { it.name == "signingKey" }
        val getClaimsFunction = kClass.memberFunctions.find { it.name == "getClaimsFromToken" }
        signingKeyProperty?.isAccessible = true
        if (signingKeyProperty is KMutableProperty1) {
            signingKeyProperty.setter.call(jwtProvider, signingKey)
        }
        getClaimsFunction?.isAccessible = true

        this.describe("새로운 토큰 생성할 때: generateToken") {
            context("아이디와 권한을 넣으면") {
                val (accessToken, refreshToken) = jwtProvider.generateToken("fooId", USER)

                it("유효한 JWT Access 토큰이 발급된다") {
                    assertDoesNotThrow { jwtProvider.validate(accessToken) }
                }
                it("유효한 JWT Refresh 토큰이 발급된다") {
                    assertDoesNotThrow { jwtProvider.validate(refreshToken) }
                }
            }

            context("추가적으로 다른 Claim 을 넣는 것이 가능하다") {
                val token = jwtProvider.generateToken("fooId", USER, "key" to "value").first

                it("유효한 JWT Access 토큰이 발급된다") {
                    val claims = getClaimsFunction?.call(jwtProvider, token) as Claims
                    assertThat(claims).extracting("key").isEqualTo("value")
                }
            }
        }

        this.describe("토큰 갱신할 때: generateToken") {
            context("유효한 JWT Refresh 토큰을 넣으면") {
                val pairs = mutableListOf("k1" to "v1", "k2" to "v2", "k3" to "v3")
                val token = jwtProvider.generateToken("fooId", USER, *pairs.toTypedArray()).first
                val oldClaims = getClaimsFunction?.call(jwtProvider, token) as Claims

                withContext(Dispatchers.IO) {
                    Thread.sleep(1000)
                }

                val (accessToken, refreshToken) = jwtProvider.generateToken(token)
                val newClaims = getClaimsFunction.call(jwtProvider, accessToken) as Claims

                it("새로운 JWT Access 토큰이 발급된다") {
                    assertDoesNotThrow { jwtProvider.validate(accessToken) }
                }
                it("새로운 JWT Refresh 토큰이 발급된다") {
                    assertDoesNotThrow { jwtProvider.validate(refreshToken) }
                }
                it("기존 JWT 토큰은 만료되지 않는다") {
                    assertDoesNotThrow { jwtProvider.validate(token) }
                }
                it("Subject, 권한 정보와 추가적인 Claim 들은 유지된다") {
                    assertThat(oldClaims.subject).isEqualTo(newClaims.subject)
                    assertThat(oldClaims["role"]).isEqualTo(newClaims["role"])
                    pairs.forAll { (key, value) ->
                        assertThat(newClaims[key]).isEqualTo(value)
                    }
                    assertThat(newClaims.size).isEqualTo(pairs.size + 4)
                }
                it("발행일자와 유효일자는 변경된다") {
                    assertThat(oldClaims.issuedAt).isNotEqualTo(newClaims.issuedAt)
                    assertThat(oldClaims.expiration).isNotEqualTo(newClaims.expiration)
                }
            }

            context("유효하지 않은 토큰을 넣으면") {
                val invalidToken = Jwts.builder().compact()

                it("InvalidJwtException 예외를 던진다") {
                    assertThrows<InvalidJwtException> { jwtProvider.generateToken(invalidToken) }
                }
            }
        }

        this.describe("토큰 검증할 때: validate") {
            val now = Date()

            context("정상적인 토큰인 경우") {
                val token = jwtProvider.generateToken("fooId", USER).first


                it("예외를 던지지 않는다") {
                    assertDoesNotThrow { jwtProvider.validate(token) }
                }
            }

            context("토큰이 Bearer 타입이 아닌 경우") {
                val invalidTypeToken = Jwts.builder()
                    .setClaims(Jwts.claims().setSubject("foo")).setIssuedAt(now)
                    .setExpiration(Date(now.time + 600000))
                    .signWith(signingKey).compact()

                it("InvalidJwtException 예외를 던진다") {
                    assertThrows<InvalidJwtException> { jwtProvider.validate(invalidTypeToken) }
                }
            }

            context("만료된 토큰인 경우") {
                val expiredToken = "Bearer " + Jwts.builder()
                    .setClaims(Jwts.claims().setSubject("foo")).setIssuedAt(now)
                    .setExpiration(now)
                    .signWith(signingKey).compact()

                it("InvalidJwtException 예외를 던진다") {
                    assertThrows<InvalidJwtException> { jwtProvider.validate(expiredToken) }
                }
            }

            context("서명되지 않은 토큰인 경우") {
                val unsignedToken = "Bearer " + Jwts.builder()
                    .setClaims(Jwts.claims().setSubject("foo")).setIssuedAt(now)
                    .setExpiration(Date(now.time + 600000)).compact()

                it("InvalidJwtException 예외를 던진다") {
                    assertThrows<InvalidJwtException> { jwtProvider.validate(unsignedToken) }
                }
            }

            context("그 외, 유효하지 않은 토큰인 경우") {
                val invalidToken = ("Bearer " + Jwts.builder()
                    .setClaims(Jwts.claims().setSubject("foo")).setIssuedAt(now)
                    .setExpiration(Date(now.time + 600000))
                    .signWith(signingKey).compact()).substring(10, 20)

                it("InvalidJwtException 예외를 던진다") {
                    assertThrows<InvalidJwtException> { jwtProvider.validate(invalidToken) }
                }
            }
        }

        this.describe("토큰으로 부터 Authentication 정보 가져올 때: getAuthenticationFromToken") {
            val activeUser = User("active", "", "", "")
            val deletedUser = User("deleted", "", "", "")
            deletedUser.delete()
            val activeUserToken = jwtProvider.generateToken("active", USER).first
            val deletedUserToken = jwtProvider.generateToken("deleted", USER).first
            val malformedUserToken = jwtProvider.generateToken("deleted", null).first

            context("DB에 해당 userId가 존재하는 경우") {
                every { userRepository.findByUserId("active") } returns activeUser

                it("WebgamAuthenticationToken 객체를 반환한다") {
                    assertThat(jwtProvider.getAuthenticationFromToken(activeUserToken)).isInstanceOf(WebgamAuthenticationToken::class.java)
                }
            }

            context("DB에 해당 userId가 존재하지만 삭제된 경우") {
                every { userRepository.findByUserId("deleted") } returns deletedUser

                it("InvalidJwtException 예외를 던진다") {
                    assertThrows<InvalidJwtException> { jwtProvider.getAuthenticationFromToken(deletedUserToken) }
                }
            }

            context("DB에 해당 userId가 존재하지 않는 경우") {
                every { userRepository.findByUserId("active") } returns null

                it("InvalidJwtException 예외를 던진다") {
                    assertThrows<InvalidJwtException> { jwtProvider.getAuthenticationFromToken(activeUserToken) }
                }
            }

            context("권한이 없는 경우") {
                it("InvalidJwtException 예외를 던진다") {
                    assertThrows<InvalidJwtException> { jwtProvider.getAuthenticationFromToken(malformedUserToken) }
                }
            }
        }
    }
}

