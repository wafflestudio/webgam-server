package com.wafflestudio.webgam.global.security

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentRequest
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentResponse
import com.wafflestudio.webgam.RestDocsUtils.Companion.requestBody
import com.wafflestudio.webgam.STRING
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.security.dto.AuthDto.*
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import com.wafflestudio.webgam.global.security.model.WebgamRoles.USER
import com.wafflestudio.webgam.type
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.extensions.spring.SpringExtension
import jakarta.servlet.http.Cookie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.mockito.BDDMockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName
import org.springframework.restdocs.cookies.CookieDocumentation.requestCookies
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ActiveProfiles("test")
@Tag("Integration-Test")
@DisplayName("Auth 통합 테스트")
internal class AuthDescribeSpec(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val jwtProvider: JwtProvider
): DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    companion object {
        private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        private val validSignupRequest = SignupRequest("fooId", "foo", "foo@wafflestudio.com", "password")
        private val invalidSignupRequest = SignupRequest("", "", "foo", "")
        private val duplicateSignupRequest = SignupRequest("duplicate-id", "foo", "foo@wafflestudio.com", "password")
        private val validLoginRequest = LoginRequest("fooId", "password")
        private val invalidLoginRequest = LoginRequest("fooId", "wrong-password")
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        withContext(Dispatchers.IO) {
            userRepository.deleteAll()
        }
    }

    init {
        this.describe("회원가입할 때") {
            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(
                        post("/signup")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(validSignupRequest))
                    ).andDo(document(
                        "signup/200",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestBody(
                            "user_id" type STRING means "로그인 할 유저 아이디",
                            "username" type STRING means "유저 네임",
                            "email" type STRING means "이메일" formattedAs "email@domain.com",
                            "password" type STRING means "로그인 비밀번호",
                        )
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("필요항목을 누락하면") {
                it("400 Bad Request, 에러코드 1") {
                    mockMvc.perform(
                        post("/signup")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(invalidSignupRequest))
                    ).andDo(document("signup/400-0001", getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(1))
                    ).andDo(print())
                }
            }

            context("이미 존재하는 아이디이거나 이메일이면") {
                withContext(Dispatchers.IO) {
                    userRepository.save(User("duplicate-id", "foo", "unqiue@wafflestudio.com", "password"))
                }

                it("409 Conflict, 에러코드 9001") {
                    mockMvc.perform(
                        post("/signup")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(duplicateSignupRequest))
                    ).andDo(document("signup/409-9001", getDocumentResponse())
                    ).andExpect(status().isConflict
                    ).andExpect(jsonPath("$.error_code", `is`(9001))
                    ).andDo(print())
                }
            }
        }

        this.describe("로그인할 때") {
            withContext(Dispatchers.IO) {
                userRepository.save(User("fooId", "foo", "unqiue@wafflestudio.com", passwordEncoder.encode("password")))
            }

            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(
                        post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(validLoginRequest))
                    ).andDo(document("login/200",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestBody(
                            "user_id" type STRING means "로그인 할 유저 아이디",
                            "password" type STRING means "로그인 비밀번호",
                        ))
                    ).andExpect(status().isOk).andDo(print())
                }
            }

            context("실패하면") {
                it("401 Unauthorized, 에러코드 1001") {
                    mockMvc.perform(
                        post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(invalidLoginRequest))
                    ).andDo(document("login/401-1001",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestBody(
                            "user_id" type STRING means "로그인 할 유저 아이디",
                            "password" type STRING means "로그인 비밀번호",
                        ))
                    ).andExpect(status().isUnauthorized
                    ).andExpect(jsonPath("$.error_code", `is`(1001))
                    ).andDo(print())
                }
            }
        }

        this.describe("토큰 갱신할 때") {
            withContext(Dispatchers.IO) {
                userRepository.save(User("foo", "foo", "unqiue@wafflestudio.com", passwordEncoder.encode("password")))
            }

            context("성공하면") {
                it("200 OK") {
                    val refreshToken = jwtProvider.generateToken("fooId", USER).second

                    mockMvc.perform(
                        post("/refresh")
                            .cookie(Cookie("refresh_token", refreshToken))
                    ).andDo(document("refresh/200",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestCookies(
                            cookieWithName("refresh_token").description("JWT Refresh 토큰: 로그인이나 회원가입 후에 자동으로 세팅됩니다.")
                        ))
                    ).andExpect(status().isOk).andDo(print())
                }
            }

            context("JWT Refresh 토큰이 없을 때") {
                it("400 Bad Request, 에러코드 2") {
                    mockMvc.perform(
                        post("/refresh")
                    ).andDo(document("refresh/400-0002-no-cookie", getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(2))
                    ).andDo(print())
                }
            }

            context("쿠키에 다른 값이 들어있을 때") {
                it("400 Bad Request, 에러코드 2") {
                    mockMvc.perform(
                        post("/refresh")
                            .cookie(Cookie("something", "something"))
                    ).andDo(document("refresh/400-0002-no-token", getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(2))
                    ).andDo(print())
                }
            }

            context("JWT Refresh 토큰이 유효하지 않을 때") {
                it("401 Unauthorized, 에러코드 1002") {
                    mockMvc.perform(
                        post("/refresh")
                            .cookie(Cookie("refresh_token", "something"))
                    ).andDo(document("refresh/401-1002", getDocumentResponse())
                    ).andExpect(status().isUnauthorized
                    ).andExpect(jsonPath("$.error_code", `is`(1002))
                    ).andDo(print())
                }
            }
        }

        this.describe("다른 API 호출할 때") {
            context("토큰 정보가 유효하고 권한이 있으면") {
                withContext(Dispatchers.IO) {
                    userRepository.save(User("fooId", "", "", ""))
                }

                it("200 OK") {
                    val accessToken = jwtProvider.generateToken("fooId", USER).first

                    mockMvc.perform(
                        get("/auth-ping").header("Authorization", accessToken)
                    ).andDo(document(
                        "auth-ping/200",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestHeaders(headerWithName("Authorization").description("JWT Access 토큰")))
                    ).andExpect(status().isOk).andDo(print())
                }
            }

            context("해당 API 가 열려 있으면") {
                it("200 OK") {
                    mockMvc.perform(
                        get("/ping")
                    ).andDo(document(
                        "ping/200",
                        getDocumentRequest(),
                        getDocumentResponse())
                    ).andExpect(status().isOk).andDo(print())
                }
            }

            context("토큰이 없으면") {
                it("401 Unauthorized, 에러코드 1000") {
                    mockMvc.perform(
                        get("/auth-ping")
                    ).andDo(document(
                        "auth-ping/401-1000",
                        getDocumentResponse())
                    ).andExpect(status().isUnauthorized
                    ).andExpect(jsonPath("$.error_code", `is`(1000))
                    ).andDo(print())
                }
            }

            context("토큰이 유효하지 않으면") {
                it("401 Unauthorized, 에러코드 1002") {
                    mockMvc.perform(
                        get("/auth-ping").header("Authorization", "invalid")
                    ).andDo(document(
                        "auth-ping/401-1002",
                        getDocumentResponse())
                    ).andExpect(status().isUnauthorized
                    ).andExpect(jsonPath("$.error_code", `is`(1002))
                    ).andDo(print())
                }
            }

            context("토큰 정보가 유효해도 권한이 없으면") {
                withContext(Dispatchers.IO) {
                    userRepository.save(User("fooId", "", "", ""))
                }

                it("403 Forbidden, 에러코드 3001") {
                    val accessToken = jwtProvider.generateToken("fooId", USER).first

                    mockMvc.perform(
                        get("/something").header("Authorization", accessToken)
                    ).andDo(document(
                        "auth-ping/403-3001",
                        getDocumentResponse())
                    ).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", `is`(3001))
                    ).andDo(print())
                }
            }
        }
    }
}