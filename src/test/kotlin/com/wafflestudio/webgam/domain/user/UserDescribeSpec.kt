package com.wafflestudio.webgam.domain.user

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentRequest
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentResponse
import com.wafflestudio.webgam.RestDocsUtils.Companion.requestBody
import com.wafflestudio.webgam.STRING
import com.wafflestudio.webgam.domain.user.dto.UserDto
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.CONSTRAINT_VIOLATION
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.INVALID_FIELD
import com.wafflestudio.webgam.global.common.exception.ErrorType.NotFound.USER_NOT_FOUND
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import com.wafflestudio.webgam.type
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.extensions.spring.SpringExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ActiveProfiles("test")
@Tag("Integration-Test")
@DisplayName("User ?????? ?????????")
class UserDescribeSpec(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val userRepository: UserRepository,
): DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    companion object {
        private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        private val validPatchRequest = UserDto.PatchRequest("new_foo", "foo2@wafflestudio.com")
        private val invalidPatchRequest = UserDto.PatchRequest("  ", "  ")
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        withContext(Dispatchers.IO) {
            userRepository.deleteAll()
        }
    }

    init {
        this.describe("??? ?????? ????????? ???") {
            context("????????????") {
                val user = withContext(Dispatchers.IO) {
                    userRepository.save(User("fooId", "foo", "foo@wafflestudio.com", ""))
                }

                it("200 OK") {
                    mockMvc.perform(
                        get("/api/v1/users/me")
                            .with(authentication(WebgamAuthenticationToken(UserPrincipal(user), "")))
                    ).andDo(document(
                        "getMe/200",
                        getDocumentRequest(),
                        getDocumentResponse()
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("??? ?????? ????????? ???") {
            context("????????????") {
                val user = withContext(Dispatchers.IO) {
                    userRepository.save(User("fooId", "foo", "foo@wafflestudio.com", ""))
                }

                it("200 OK") {
                    mockMvc.perform(
                        patch("/api/v1/users/me")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(validPatchRequest))
                            .with(authentication(WebgamAuthenticationToken(UserPrincipal(user), "")))
                    ).andDo(document(
                        "patchMe/200",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestBody(
                            "username" type STRING means "????????????" formattedAs "??? ????????? X" isOptional true,
                            "email" type STRING means "?????????" formattedAs "??? ????????? X, ????????? ????????? ??????" isOptional true
                        ))
                    ).andExpect(status().isOk).andDo(print())
                }
            }

            context("???????????? ?????? ?????? ????????? ???") {
                val user = withContext(Dispatchers.IO) {
                    userRepository.save(User("fooId", "foo", "foo@wafflestudio.com", ""))
                }

                it("400 Bad Request, ???????????? ") {
                    mockMvc.perform(
                        patch("/api/v1/users/me")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(invalidPatchRequest))
                            .with(authentication(WebgamAuthenticationToken(UserPrincipal(user), "")))
                    ).andDo(document(
                        "patchMe/400-0001",
                        getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(INVALID_FIELD.code()))
                    ).andDo(print())
                }
            }
        }

        this.describe("?????? ????????? ???") {
            context("????????????") {
                val user = withContext(Dispatchers.IO) {
                    userRepository.save(User("fooId", "foo", "foo@wafflestudio.com", ""))
                }

                it("200 OK") {
                    mockMvc.perform(
                        delete("/api/v1/users/me")
                            .with(authentication(WebgamAuthenticationToken(UserPrincipal(user), "")))
                    ).andDo(document(
                        "deleteMe/200",
                        getDocumentRequest(),
                        getDocumentResponse())
                    ).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("?????? ?????? ?????? ????????? ???") {
            val loginUser = withContext(Dispatchers.IO) {
                userRepository.save(User("fooId", "foo", "foo@wafflestudio.com", ""))
            }

            val user = withContext(Dispatchers.IO) {
                userRepository.save(User("barId", "bar", "bar@wafflestudio.com", ""))
            }

            context("????????????") {
                it("200 OK") {
                    mockMvc.perform(
                        get("/api/v1/users/{id}", "${user.id}")
                            .with(authentication(WebgamAuthenticationToken(UserPrincipal(loginUser), "")))
                    ).andDo(document(
                        "getUser/200",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        pathParameters(parameterWithName("id").description("????????? ?????? ID")))
                    ).andExpect(status().isOk).andDo(print())
                }
            }

            context("?????? ID??? ?????? ???????????? ??????") {
                it("400 Bad Request, ???????????? ${CONSTRAINT_VIOLATION.code()}") {
                    mockMvc.perform(
                        get("/api/v1/users/{id}", "0")
                            .with(authentication(WebgamAuthenticationToken(UserPrincipal(loginUser), "")))
                    ).andDo(document(
                        "getUser/400-0003",
                        getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(CONSTRAINT_VIOLATION.code()))
                    ).andDo(print())
                }
            }

            context("???????????? ????????? ????????? ????????? ???????????? ??????") {
                it("404 Not Found, ???????????? ${USER_NOT_FOUND.code()}") {
                    mockMvc.perform(
                        get("/api/v1/users/{id}", (user.id + 33).toString())
                            .with(authentication(WebgamAuthenticationToken(UserPrincipal(loginUser), "")))
                    ).andDo(document(
                        "getUser/404-4001",
                        getDocumentResponse())
                    ).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", `is`(USER_NOT_FOUND.code()))
                    ).andDo(print())
                }
            }
        }
    }
}