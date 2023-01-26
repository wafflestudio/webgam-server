package com.wafflestudio.webgam.global.security.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.ninjasquad.springmockk.MockkBean
import com.wafflestudio.webgam.domain.user.dto.UserDto.SimpleResponse
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.global.security.dto.AuthDto.Response
import com.wafflestudio.webgam.global.security.dto.JwtDto
import com.wafflestudio.webgam.global.security.exception.InvalidJwtException
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
                it("400 Bad Request, 에러코드 1") {
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
                            jsonPath("$.error_code", `is`(1))
                        }
                    }
                }
            }

            context("유저네임을 누락하거나 공백, NULL 이면") {
                it("400 Bad Request, 에러코드 1") {
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
                            jsonPath("$.error_code", `is`(1))
                        }
                    }
                }
            }

            context("이메일이 누락하거나 공백, NULL, 잘못된 형식이면") {
                it("400 Bad Request, 에러코드 1") {
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
                            jsonPath("$.error_code", `is`(1))
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
                it("400 Bad Request, 에러코드 1") {
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
                            jsonPath("$.error_code", `is`(1))
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
                it("400 Bad Request, 에러코드 1") {
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
                            jsonPath("$.error_code", `is`(1))
                        }
                    }
                }
            }

            context("비밀번호가 누락하거나 공백, NULL 이면") {
                it("400 Bad Request, 에러코드 1") {
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
                            jsonPath("$.error_code", `is`(1))
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
                it("400 Bad Request, 에러코드 2") {
                    mockMvc.post("/refresh").andDo { print() }.andExpect {
                        status { isBadRequest() }
                        jsonPath("$.error_code", `is`(2))
                    }
                }
            }

            context("유효하지 않은 refreshToken 쿠키가 있으면") {
                every { authService.refreshToken(any()) } throws InvalidJwtException("message")

                it("401 Unauthorized, 에러코드 1002") {
                    mockMvc.post("/refresh") {
                        cookie(Cookie("refresh_token", "dummy_valid_token"))
                    }.andExpect {
                        status { isUnauthorized() }
                        jsonPath("$.error_code", `is`(1002))
                    }
                }
            }
        }
    }
}