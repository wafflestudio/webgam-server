package com.wafflestudio.webgam.domain.user.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.ninjasquad.springmockk.MockkBean
import com.wafflestudio.webgam.domain.user.dto.UserDto.SimpleResponse
import com.wafflestudio.webgam.domain.user.exception.UserNotFoundException
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.service.UserService
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.CONSTRAINT_VIOLATION
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.INVALID_FIELD
import com.wafflestudio.webgam.global.common.exception.ErrorType.NotFound.USER_NOT_FOUND
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.inspectors.forAll
import io.mockk.every
import io.mockk.justRun
import org.hamcrest.core.Is
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.*

@WebMvcTest(UserController::class)
@MockkBean(JpaMetamodelMappingContext::class)
@ActiveProfiles("test")
@Tag("Unit-Test")
@DisplayName("UserController 단위 테스트")
class UserControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @MockkBean private val userService: UserService,
) : DescribeSpec() {

    companion object {
        private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        private val user = User("", "", "", "")
        private val authentication = WebgamAuthenticationToken(UserPrincipal(user), "")
        private val dummyUser = User("", "", "", "")
    }

    init {
        this.describe("내 정보 조회할 때") {
            every { userService.getMe(any()) } returns SimpleResponse(user)

            context("성공하면") {
                it("200 OK") {
                    mockMvc.get("/api/v1/users/me") {
                        with(authentication(authentication))
                    }.andExpect { status { isOk() } }
                }
            }
        }

        this.describe("내 정보 수정할 때") {
            every { userService.patchMe(any(), any()) } returns SimpleResponse(user)

            val validUsernames = mutableListOf("omit", "foo", null)
            val invalidUsernames = mutableListOf("", "     ")
            val validEmails = mutableListOf("omit", "foo@wafflestudio.com")
            val invalidEmails = mutableListOf("foo", "@")

            context("적절한 유저네임과 이메일을 입력하면") {
                val tuples = validUsernames.map { u -> validEmails.map { e -> u to e } }.flatten()

                it("200 OK") {
                    tuples.forAll { (username, email) ->
                        val request = HashMap<String, String?>()
                        if (username != "omit") request["username"] = username
                        if (email != "omit") request["email"] = email

                        mockMvc.patch("/api/v1/users/me") {
                            contentType = MediaType.APPLICATION_JSON
                            content = gson.toJson(request)
                            with(authentication(authentication))
                            with(csrf())
                        }.andExpect { status { isOk() } }
                    }
                }
            }

            context("적절하지 않은 유저네임을 입력하면") {
                val tuples = invalidUsernames.map { u -> validEmails.map { e -> u to e } }.flatten()

                it("400 Bad Request, 에러코드 ${INVALID_FIELD.code()}") {
                    tuples.forAll { (username, email) ->
                        val request = HashMap<String, String?>()
                        if (username != "omit") request["username"] = username
                        if (email != "omit") request["email"] = email

                        mockMvc.patch("/api/v1/users/me") {
                            contentType = MediaType.APPLICATION_JSON
                            content = gson.toJson(request)
                            with(authentication(authentication))
                            with(csrf())
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", Is.`is`(INVALID_FIELD.code()))
                        }
                    }
                }
            }

            context("적절하지 않은 이메일을 입력하면") {
                val tuples = validUsernames.map { u -> invalidEmails.map { e -> u to e } }.flatten()

                it("400 Bad Request, 에러코드 ${INVALID_FIELD.code()}") {
                    tuples.forAll { (username, email) ->
                        val request = HashMap<String, String?>()
                        if (username != "omit") request["username"] = username
                        if (email != "omit") request["email"] = email

                        mockMvc.patch("/api/v1/users/me") {
                            contentType = MediaType.APPLICATION_JSON
                            content = gson.toJson(request)
                            with(authentication(authentication))
                            with(csrf())
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", Is.`is`(INVALID_FIELD.code()))
                        }
                    }
                }
            }
        }

        this.describe("내 정보 삭제(탈퇴)할 때") {
            justRun { userService.deleteMe(any()) }

            context("성공하면") {
                it("200 OK") {
                    mockMvc.delete("/api/v1/users/me") {
                        with(authentication(authentication))
                        with(csrf())
                    }.andExpect { status { isOk() } }
                }
            }
        }

        this.describe("다른 유저 정보 조회할 때") {
            context("존재하는 유저 ID를 요청하면") {
                every { userService.getUserWithId(1) } returns SimpleResponse(dummyUser)

                it("200 OK") {
                    mockMvc.get("/api/v1/users/{id}", "1") {
                        with(authentication(authentication))
                    }.andExpect { status { isOk() } }
                }
            }

            context("존재하지 않거나 탈퇴한 유저 ID를 요청하면") {
                every { userService.getUserWithId(1) } throws UserNotFoundException(1)

                it("404 Not Found, 에러코드 ${USER_NOT_FOUND.code()}") {
                    mockMvc.get("/api/v1/users/{id}", "1") {
                        with(authentication(authentication))
                    }.andExpect {
                        status { isNotFound() }
                        jsonPath("$.error_code", Is.`is`(USER_NOT_FOUND.code()))
                    }
                }
            }

            context("ID가 올바르지 않으면") {
                val userIds = mutableListOf(-1, 0)

                it("400 Bad Request, 에러코드 ${CONSTRAINT_VIOLATION.code()}") {
                    userIds.forAll { id ->
                        mockMvc.get("/api/v1/users/{id}", "$id") {
                            with(authentication(authentication))
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", Is.`is`(CONSTRAINT_VIOLATION.code()))
                        }
                    }
                }
            }
        }
    }
}
