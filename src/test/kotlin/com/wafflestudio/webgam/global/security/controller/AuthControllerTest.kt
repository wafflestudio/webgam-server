package com.wafflestudio.webgam.global.security.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.ninjasquad.springmockk.MockkBean
import com.wafflestudio.webgam.domain.user.dto.UserDto.SimpleResponse
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.INVALID_FIELD
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.NO_REFRESH_TOKEN
import com.wafflestudio.webgam.global.common.exception.ErrorType.Unauthorized.INVALID_JWT
import com.wafflestudio.webgam.global.security.dto.AuthDto.Response
import com.wafflestudio.webgam.global.security.dto.JwtDto
import com.wafflestudio.webgam.global.security.exception.InvalidJwtException
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import com.wafflestudio.webgam.global.security.service.AuthService
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.inspectors.forAll
import io.mockk.every
import jakarta.servlet.http.Cookie
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(AuthController::class, excludeAutoConfiguration = [SecurityAutoConfiguration::class])
@MockkBean(JpaMetamodelMappingContext::class)
@ActiveProfiles("test")
@Tag("Unit-Test")
@DisplayName("AuthController 단위 테스트")
class AuthControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @MockkBean private val authService: AuthService
) : DescribeSpec() {

    companion object {
        private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        private val dummyUser = User("", "", "", "")
        private val dummyUserResponse = SimpleResponse(dummyUser)
    }

    init {
        this.describe("회원가입할 때") {
            every { authService.signup(any()) } returns (Response(dummyUserResponse, "message","access_token") to "refresh_token")

            context("적절한 아이디, 유저네임, 이메일, 비밀번호를 입력하면") {
                val request = HashMap<String, String>()
                request["user_id"] = "fooId"
                request["username"] = "foo"
                request["email"] = "foo@wafflestudio.com"
                request["password"] = "foo-password"

                it("200 OK") {
                    mockMvc.post("/signup") {
                        contentType = APPLICATION_JSON
                        content = gson.toJson(request)
                    }.andExpect { status { isOk() } }
                }
            }

            context("그 외 추가적인 항목이 있어도") {
                val request = HashMap<String, String>()
                request["user_id"] = "fooId"
                request["username"] = "foo"
                request["email"] = "foo@wafflestudio.com"
                request["password"] = "foo-password"
                request["extra"] = "something"

                it("200 OK") {
                    mockMvc.post("/signup") {
                        contentType = APPLICATION_JSON
                        content = gson.toJson(request)
                    }.andExpect { status { isOk() } }
                }
            }

            context("아이디를 누락하거나 공백, NULL 이면") {
                it("400 Bad Request, 에러코드 ${INVALID_FIELD.code()}") {
                    val userIds = mutableListOf("omit", "", "    ", null)
                    userIds.forAll {
                        val request = HashMap<String, String?>()
                        if (it != "omit") request["user_id"] = it
                        request["username"] = "foo"
                        request["email"] = "foo@wafflestudio.com"
                        request["password"] = "foo-password"
                        mockMvc.post("/signup") {
                            contentType = APPLICATION_JSON
                            content = gson.toJson(request)
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", `is`(INVALID_FIELD.code()))
                        }
                    }
                }
            }

            context("유저네임을 누락하거나 공백, NULL 이면") {
                it("400 Bad Request, 에러코드 ${INVALID_FIELD.code()}") {
                    val usernames = mutableListOf("omit", "", "    ", null)
                    usernames.forAll {
                        val request = HashMap<String, String?>()
                        request["user_id"] = "fooId"
                        if (it != "omit") request["username"] = it
                        request["email"] = "foo@wafflestudio.com"
                        request["password"] = "foo-password"
                        mockMvc.post("/signup") {
                            contentType = APPLICATION_JSON
                            content = gson.toJson(request)
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", `is`(INVALID_FIELD.code()))
                        }
                    }
                }
            }

            context("이메일이 누락하거나 공백, NULL, 잘못된 형식이면") {
                it("400 Bad Request, 에러코드 ${INVALID_FIELD.code()}") {
                    val emails = mutableListOf("omit", "", "    ", null, "email", "@")
                    emails.forAll {
                        val request = HashMap<String, String?>()
                        request["user_id"] = "fooId"
                        request["username"] = "foo"
                        if (it != "omit") request["email"] = it
                        request["password"] = "foo-password"
                        mockMvc.post("/signup") {
                            contentType = APPLICATION_JSON
                            content = gson.toJson(request)
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", `is`(INVALID_FIELD.code()))
                        }
                    }
                }
            }

            context("이메일에 @만 포함되어 있으면") {
                it("200 OK") {
                    val emails = mutableListOf("1@a", "email@domain")
                    emails.forAll {
                        val request = HashMap<String, String?>()
                        request["user_id"] = "fooId"
                        request["username"] = "foo"
                        request["email"] = it
                        request["password"] = "foo-password"
                        mockMvc.post("/signup") {
                            contentType = APPLICATION_JSON
                            content = gson.toJson(request)
                        }.andExpect { status { isOk() } }
                    }
                }
            }

            context("비밀번호를 누락하거나 공백, NULL 이면") {
                it("400 Bad Request, 에러코드 ${INVALID_FIELD.code()}") {
                    val passwords = mutableListOf("omit", "", "    ", null)
                    passwords.forAll {
                        val request = HashMap<String, String?>()
                        request["user_id"] = "fooId"
                        request["username"] = "foo"
                        request["email"] = "foo@wafflestudio.com"
                        if (it != "omit") request["password"] = it
                        mockMvc.post("/signup") {
                            contentType = APPLICATION_JSON
                            content = gson.toJson(request)
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", `is`(INVALID_FIELD.code()))
                        }
                    }
                }
            }
        }

        this.describe("로그인할 때") {
            every { authService.login(any()) } returns (Response(dummyUserResponse, "message","access_token") to "refresh_token")

            context("적절한 아이디, 비밀번호를 입력하면") {
                val request = HashMap<String, String>()
                request["user_id"] = "fooId"
                request["password"] = "foo-password"

                it("200 OK") {
                    mockMvc.post("/login") {
                        contentType = APPLICATION_JSON
                        content = gson.toJson(request)
                    }.andExpect { status { isOk() } }
                }
            }

            context("적절한 아이디, 비밀번호와 함께 자동 로그인 요청하면") {
                val request = HashMap<String, String>()
                request["user_id"] = "fooId"
                request["password"] = "foo-password"
                request["auto"] = "true"

                it("Refresh 토큰이 영구 쿠키로 반환된다") {
                    mockMvc.post("/login") {
                        contentType = APPLICATION_JSON
                        content = gson.toJson(request)
                    }.andExpect {
                        status { isOk() }
                        cookie { maxAge("refresh_token", `is`(JwtProvider.refreshTokenValidTime.toInt())) }
                    }
                }
            }

            context("자동 로그인을 따로 설정하지 않거나 옵션 해제하면") {
                val request = HashMap<String, String>()
                request["user_id"] = "fooId"
                request["password"] = "foo-password"
                request["auto"] = "false"

                it("Refresh 토큰이 세션 쿠키로 반환된다") {
                    mockMvc.post("/login") {
                        contentType = APPLICATION_JSON
                        content = gson.toJson(request)
                    }.andExpect {
                        status { isOk() }
                        cookie { maxAge("refresh_token", `is`(-1)) }
                    }
                }
            }

            context("그 외 추가적인 항목이 있어도") {
                val request = HashMap<String, String>()
                request["user_id"] = "fooId"
                request["password"] = "foo-password"
                request["extra"] = "something"

                it("200 OK") {
                    mockMvc.post("/login") {
                        contentType = APPLICATION_JSON
                        content = gson.toJson(request)
                    }.andExpect { status { isOk() } }
                }
            }

            context("아이디를 누락하거나 공백, NULL 이면") {
                it("400 Bad Request, 에러코드 ${INVALID_FIELD.code()}") {
                    val userIds = mutableListOf("omit", "", "    ", null)
                    userIds.forAll {
                        val request = HashMap<String, String?>()
                        if (it != "omit") request["user_id"] = it
                        request["password"] = "foo-password"
                        mockMvc.post("/login") {
                            contentType = APPLICATION_JSON
                            content = gson.toJson(request)
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", `is`(INVALID_FIELD.code()))
                        }
                    }
                }
            }

            context("비밀번호가 누락하거나 공백, NULL 이면") {
                it("400 Bad Request, 에러코드 ${INVALID_FIELD.code()}") {
                    val passwords = mutableListOf("omit", "", "    ", null)
                    passwords.forAll {
                        val request = HashMap<String, String?>()
                        request["user_id"] = "fooId"
                        if (it != "omit") request["password"] = it
                        mockMvc.post("/login") {
                            contentType = APPLICATION_JSON
                            content = gson.toJson(request)
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", `is`(INVALID_FIELD.code()))
                        }
                    }
                }
            }
        }

        this.describe("JWT Access 토큰 재발급받을 때") {
            context("유효한 refreshToken 쿠키가 있으면") {
                every { authService.refreshToken(any()) } returns (JwtDto.AccessToken("access_token") to "refresh_token")

                it("200 OK") {
                    mockMvc.post("/refresh") {
                        cookie(Cookie("refresh_token", "dummy_valid_token"))
                    }.andExpect { status { isOk() } }
                }
            }

            context("refreshToken 쿠키가 없으면") {
                it("400 Bad Request, 에러코드 ${NO_REFRESH_TOKEN.code()}") {
                    mockMvc.post("/refresh").andDo { print() }.andExpect {
                        status { isBadRequest() }
                        jsonPath("$.error_code", `is`(NO_REFRESH_TOKEN.code()))
                    }
                }
            }

            context("유효하지 않은 refreshToken 쿠키가 있으면") {
                every { authService.refreshToken(any()) } throws InvalidJwtException("message")

                it("401 Unauthorized, 에러코드 ${INVALID_JWT.code()}") {
                    mockMvc.post("/refresh") {
                        cookie(Cookie("refresh_token", "dummy_valid_token"))
                    }.andExpect {
                        status { isUnauthorized() }
                        jsonPath("$.error_code", `is`(INVALID_JWT.code()))
                    }
                }
            }
        }
    }
}