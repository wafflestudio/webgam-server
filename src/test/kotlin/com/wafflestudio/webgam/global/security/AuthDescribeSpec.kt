package com.wafflestudio.webgam.global.security

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.wafflestudio.webgam.BOOLEAN
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentRequest
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentResponse
import com.wafflestudio.webgam.RestDocsUtils.Companion.requestBody
import com.wafflestudio.webgam.STRING
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.INVALID_FIELD
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.NO_REFRESH_TOKEN
import com.wafflestudio.webgam.global.common.exception.ErrorType.Conflict.DUPLICATE_USER_IDENTIFIER
import com.wafflestudio.webgam.global.common.exception.ErrorType.Forbidden.NO_ACCESS
import com.wafflestudio.webgam.global.common.exception.ErrorType.Unauthorized.*
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
@DisplayName("Auth ?????? ?????????")
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
        this.describe("??????????????? ???") {
            context("????????????") {
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
                            "user_id" type STRING means "????????? ??? ?????? ?????????",
                            "username" type STRING means "?????? ??????",
                            "email" type STRING means "?????????" formattedAs "email@domain.com",
                            "password" type STRING means "????????? ????????????",
                        )
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("??????????????? ????????????") {
                it("400 Bad Request, ???????????? ${INVALID_FIELD.code()}") {
                    mockMvc.perform(
                        post("/signup")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(invalidSignupRequest))
                    ).andDo(document("signup/400-0001", getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(INVALID_FIELD.code()))
                    ).andDo(print())
                }
            }

            context("?????? ???????????? ?????????????????? ???????????????") {
                withContext(Dispatchers.IO) {
                    userRepository.save(User("duplicate-id", "foo", "unqiue@wafflestudio.com", "password"))
                }

                it("409 Conflict, ???????????? ${DUPLICATE_USER_IDENTIFIER.code()}") {
                    mockMvc.perform(
                        post("/signup")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(duplicateSignupRequest))
                    ).andDo(document("signup/409-9001", getDocumentResponse())
                    ).andExpect(status().isConflict
                    ).andExpect(jsonPath("$.error_code", `is`(DUPLICATE_USER_IDENTIFIER.code()))
                    ).andDo(print())
                }
            }
        }

        this.describe("???????????? ???") {
            withContext(Dispatchers.IO) {
                userRepository.save(User("fooId", "foo", "unqiue@wafflestudio.com", passwordEncoder.encode("password")))
            }

            context("????????????") {
                it("200 OK") {
                    mockMvc.perform(
                        post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(validLoginRequest))
                    ).andDo(document("login/200",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestBody(
                            "user_id" type STRING means "????????? ??? ?????? ?????????",
                            "password" type STRING means "????????? ????????????",
                            "auto" type BOOLEAN means "?????? ????????? ??????" withDefaultValue "false" isOptional true
                        ))
                    ).andExpect(status().isOk).andDo(print())
                }
            }

            context("????????????") {
                it("401 Unauthorized, ???????????? ${LOGIN_FAIL.code()}") {
                    mockMvc.perform(
                        post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(invalidLoginRequest))
                    ).andDo(document("login/401-1001",
                        getDocumentRequest(),
                        getDocumentResponse())
                    ).andExpect(status().isUnauthorized
                    ).andExpect(jsonPath("$.error_code", `is`(LOGIN_FAIL.code()))
                    ).andDo(print())
                }
            }
        }

        this.describe("??????????????? ???") {
            context("????????????") {
                it("200 OK") {
                    mockMvc.perform(post("/logout")
                    ).andDo(document("logout/200",
                        getDocumentRequest(),
                        getDocumentResponse())
                    ).andExpect(status().isOk
                    ).andDo(print())
                }
            }
        }

        this.describe("?????? ????????? ???") {
            withContext(Dispatchers.IO) {
                userRepository.save(User("foo", "foo", "unqiue@wafflestudio.com", passwordEncoder.encode("password")))
            }

            context("????????????") {
                it("200 OK") {
                    val refreshToken = jwtProvider.generateToken("fooId", USER).second

                    mockMvc.perform(
                        post("/refresh")
                            .cookie(Cookie("refresh_token", refreshToken))
                    ).andDo(document("refresh/200",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestCookies(
                            cookieWithName("refresh_token").description("JWT Refresh ??????: ??????????????? ???????????? ?????? ???????????? ???????????????.")
                        ))
                    ).andExpect(status().isOk).andDo(print())
                }
            }

            context("JWT Refresh ????????? ?????? ???") {
                it("400 Bad Request, ???????????? ${NO_REFRESH_TOKEN.code()}") {
                    mockMvc.perform(
                        post("/refresh")
                    ).andDo(document("refresh/400-0002-no-cookie", getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(NO_REFRESH_TOKEN.code()))
                    ).andDo(print())
                }
            }

            context("????????? ?????? ?????? ???????????? ???") {
                it("400 Bad Request, ???????????? ${NO_REFRESH_TOKEN.code()}") {
                    mockMvc.perform(
                        post("/refresh")
                            .cookie(Cookie("something", "something"))
                    ).andDo(document("refresh/400-0002-no-token", getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(NO_REFRESH_TOKEN.code()))
                    ).andDo(print())
                }
            }

            context("JWT Refresh ????????? ???????????? ?????? ???") {
                it("401 Unauthorized, ???????????? ${INVALID_JWT.code()}") {
                    mockMvc.perform(
                        post("/refresh")
                            .cookie(Cookie("refresh_token", "something"))
                    ).andDo(document("refresh/401-1002", getDocumentResponse())
                    ).andExpect(status().isUnauthorized
                    ).andExpect(jsonPath("$.error_code", `is`(INVALID_JWT.code()))
                    ).andDo(print())
                }
            }
        }

        this.describe("?????? API ????????? ???") {
            context("?????? ????????? ???????????? ????????? ?????????") {
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
                        requestHeaders(headerWithName("Authorization").description("JWT Access ??????")))
                    ).andExpect(status().isOk).andDo(print())
                }
            }

            context("?????? API ??? ?????? ?????????") {
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

            context("????????? ?????????") {
                it("401 Unauthorized, ???????????? ${DEFAULT.code()}") {
                    mockMvc.perform(
                        get("/auth-ping")
                    ).andDo(document(
                        "auth-ping/401-1000",
                        getDocumentResponse())
                    ).andExpect(status().isUnauthorized
                    ).andExpect(jsonPath("$.error_code", `is`(DEFAULT.code()))
                    ).andDo(print())
                }
            }

            context("????????? ???????????? ?????????") {
                it("401 Unauthorized, ???????????? ${INVALID_JWT.code()}") {
                    mockMvc.perform(
                        get("/auth-ping").header("Authorization", "invalid")
                    ).andDo(document(
                        "auth-ping/401-1002",
                        getDocumentResponse())
                    ).andExpect(status().isUnauthorized
                    ).andExpect(jsonPath("$.error_code", `is`(INVALID_JWT.code()))
                    ).andDo(print())
                }
            }

            context("?????? ????????? ???????????? ????????? ?????????") {
                withContext(Dispatchers.IO) {
                    userRepository.save(User("fooId", "", "", ""))
                }

                it("403 Forbidden, ???????????? ${NO_ACCESS.code()}") {
                    val accessToken = jwtProvider.generateToken("fooId", USER).first

                    mockMvc.perform(
                        get("/something").header("Authorization", accessToken)
                    ).andDo(document(
                        "auth-ping/403-3001",
                        getDocumentResponse())
                    ).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", `is`(NO_ACCESS.code()))
                    ).andDo(print())
                }
            }
        }
    }
}