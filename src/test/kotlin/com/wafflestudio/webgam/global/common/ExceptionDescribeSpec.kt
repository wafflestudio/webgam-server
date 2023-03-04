package com.wafflestudio.webgam.global.common

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.ninjasquad.springmockk.MockkBean
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentResponse
import com.wafflestudio.webgam.domain.event.exception.LinkNonRelatedPageException
import com.wafflestudio.webgam.domain.event.exception.MultipleEventAllocationException
import com.wafflestudio.webgam.domain.event.exception.NonAccessibleObjectEventException
import com.wafflestudio.webgam.domain.event.exception.ObjectEventNotFoundException
import com.wafflestudio.webgam.domain.`object`.exception.NonAccessiblePageObjectException
import com.wafflestudio.webgam.domain.`object`.exception.PageObjectNotFoundException
import com.wafflestudio.webgam.domain.page.exception.NonAccessibleProjectPageException
import com.wafflestudio.webgam.domain.page.exception.ProjectPageNotFoundException
import com.wafflestudio.webgam.domain.project.exception.NonAccessibleProjectException
import com.wafflestudio.webgam.domain.project.exception.ProjectNotFoundException
import com.wafflestudio.webgam.domain.user.exception.UserNotFoundException
import com.wafflestudio.webgam.global.common.ExceptionDescribeSpec.Domain.*
import com.wafflestudio.webgam.global.common.ExceptionDescribeSpec.ExceptionTestConfig.ExceptionTestController
import com.wafflestudio.webgam.global.common.ExceptionDescribeSpec.ExceptionTestConfig.ExceptionTestService
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.*
import com.wafflestudio.webgam.global.common.exception.ErrorType.Conflict.*
import com.wafflestudio.webgam.global.common.exception.ErrorType.Forbidden.*
import com.wafflestudio.webgam.global.common.exception.ErrorType.NotFound.*
import com.wafflestudio.webgam.global.common.exception.ErrorType.Unauthorized
import com.wafflestudio.webgam.global.common.exception.ErrorType.Unauthorized.*
import com.wafflestudio.webgam.global.security.exception.*
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.hamcrest.core.Is.`is`
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.stereotype.Service
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@WebMvcTest(ExceptionTestController::class, excludeAutoConfiguration = [SecurityAutoConfiguration::class])
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ActiveProfiles("test")
@DisplayName("Exception Docs 생성")
class ExceptionDescribeSpec(
    private val mockMvc: MockMvc,
    @MockkBean private val exceptionTestService: ExceptionTestService,
): DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    @TestConfiguration
    class ExceptionTestConfig {
        @Service
        class ExceptionTestService {
            fun error() {}
        }

        @Validated
        @RestController
        @RequestMapping("/error")
        class ExceptionTestController(private val exceptionTestService: ExceptionTestService) {
            @GetMapping("/{id}")
            fun constraintViolation(@PathVariable("id") @Positive id: Long): ResponseEntity<Any> {
                return ResponseEntity.ok().build()
            }

            @GetMapping
            fun serviceError(): ResponseEntity<Any> {
                exceptionTestService.error()
                return ResponseEntity.ok().build()
            }

            @PostMapping
            fun invalidField(@RequestBody @Valid request: ExceptionTestDto): ResponseEntity<Any> {
                return ResponseEntity.ok().build()
            }

            @GetMapping("/params")
            fun parameterTypeMismatch(@RequestParam int: Int): ResponseEntity<Any> {
                return ResponseEntity.ok().build()
            }
        }

        data class ExceptionTestDto(
            @field:[NotBlank]
            private val notBlankField: String?,
            @field:[NotNull Positive]
            private val nonNullablePositiveField: Int,
            @field:[Positive]
            private val nullablePositiveField: Int?,
            private val enumField: ExceptionEnum?
        )

        enum class ExceptionEnum {
            @JsonProperty("PROPER")
            PROPER
        }
    }

    private enum class Domain(val code: Int) {
        COMMON(0),
        USER(0),
        AUTH(0),
        PROJECT(100),
        PAGE(200),
        OBJECT(300),
        EVENT(400),
    }

    private infix fun Int.shouldBeDomainOf(domain: Domain) {
        val domainCode = this / 100 % 10 * 100
        domainCode shouldBe domain.code
    }

    init {
        this.describe("COMMON") {
            COMMON.code shouldBe 0

            context("RequestBody에 유효하지 않은 값이 들어올 때") {
                val code = INVALID_FIELD.code()
                val request = mapOf(
                    "nullable_positive_field" to 0
                )

                it("에러코드 $code") {
                    mockMvc.perform(
                        post("/error")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(gson.toJson(request))
                    ).andDo(document(
                        "error/common/1",
                        getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf COMMON
                }
            }

            context("PathVariable에 유효하지 않은 값이 들어올 때") {
                val code = CONSTRAINT_VIOLATION.code()

                it("에러코드 $code") {
                    mockMvc.perform(
                        get("/error/{id}", "-1")
                    ).andDo(document(
                        "error/common/2",
                        getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf COMMON
                }
            }

            context("RequestBody에 올바르지 않은 Type이 들어올 때") {
                val code = JSON_PARSE_ERROR.code()
                val request = mapOf(
                    "enum_field" to "something"
                )

                it("에러코드 $code") {
                    mockMvc.perform(
                        post("/error")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(gson.toJson(request))
                    ).andDo(document(
                        "error/common/3",
                        getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf COMMON
                }
            }

            context("RequestParam에 올바르지 않은 Type이 들어올 때") {
                val code = PARAMETER_TYPE_MISMATCH.code()

                it("에러코드 $code") {
                    mockMvc.perform(
                        get("/error/params")
                            .param("int", "nonInt")
                    ).andDo(document(
                        "error/common/4",
                        getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf COMMON
                }
            }
        }

        this.describe("인증 도메인") {
            AUTH.code shouldBe 0

            context("토큰이 없는 경우") {
                every { exceptionTestService.error() } throws UnauthorizedException()

                val code = Unauthorized.DEFAULT.code()
                it("DEFAULT: 에러코드 $code") {
                    mockMvc.perform(
                        get("/error")
                    ).andDo(document(
                        "error/auth/1",
                        getDocumentResponse())
                    ).andExpect(status().isUnauthorized
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf AUTH
                }
            }

            context("토큰이 유효하지 않는 경우") {
                every { exceptionTestService.error() } throws InvalidJwtException("Invalid JWT token.")

                val code = INVALID_JWT.code()
                it("INVALID_JWT: 에러코드 $code") {
                    mockMvc.perform(
                        get("/error")
                    ).andDo(document(
                        "error/auth/2",
                        getDocumentResponse())
                    ).andExpect(status().isUnauthorized
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf AUTH
                }
            }

            context("토큰이 유효하지만, 해당 API나 리소스에 접근 권한 없는 경우") {
                every { exceptionTestService.error() } throws NoAccessException()

                val code = NO_ACCESS.code()
                it("NO_ACCESS: 에러코드 $code") {
                    mockMvc.perform(
                        get("/error")
                    ).andDo(document(
                        "error/auth/3",
                        getDocumentResponse())
                    ).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf AUTH
                }
            }

            context("로그인 실패한 경우") {
                every { exceptionTestService.error() } throws LoginFailedException()

                val code = LOGIN_FAIL.code()
                it("LOGIN_FAIL: 에러코드 $code") {
                    mockMvc.perform(
                        get("/error")
                    ).andDo(document(
                        "error/auth/4",
                        getDocumentResponse())
                    ).andExpect(status().isUnauthorized
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf AUTH
                }
            }

            context("동일한 유저ID나 이메일이 존재하는 경우") {
                every { exceptionTestService.error() } throws DuplicateUserIdentifierException()

                val code = DUPLICATE_USER_IDENTIFIER.code()
                it("DUPLICATE_USER_IDENTIFIER: 에러코드 $code") {
                    mockMvc.perform(
                        get("/error")
                    ).andDo(document(
                        "error/auth/5",
                        getDocumentResponse())
                    ).andExpect(status().isConflict
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf AUTH
                }
            }

            context("Refresh 토큰이 없는데 재발급 요청하는 경우") {
                every { exceptionTestService.error() } throws NoRefreshTokenException()

                val code = NO_REFRESH_TOKEN.code()
                it("NO_REFRESH_TOKEN: 에러코드 $code") {
                    mockMvc.perform(
                        get("/error")
                    ).andDo(document(
                        "error/auth/6",
                        getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf AUTH
                }
            }
        }

        this.describe("User 도메인") {
            USER.code shouldBe 0

            context("유저가 탈퇴했거나 존재하지 않을 때") {
                every { exceptionTestService.error() } throws UserNotFoundException(1L)

                val code = USER_NOT_FOUND.code()
                it("USER_NOT_FOUND: 에러코드 $code") {
                    mockMvc.perform(get("/error")
                    ).andDo(document(
                        "error/user/1",
                        getDocumentResponse())
                    ).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf USER
                }
            }
        }

        this.describe("Project 도메인") {
            PROJECT.code shouldBe 100

            context("프로젝트가 삭제되거나 존재하지 않을 때") {
                every { exceptionTestService.error() } throws ProjectNotFoundException(1L)

                val code = PROJECT_NOT_FOUND.code()
                it("PROJECT_NOT_FOUND: 에러코드 $code") {
                    mockMvc.perform(get("/error")
                    ).andDo(document(
                        "error/project/1",
                        getDocumentResponse())
                    ).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf PROJECT
                }
            }

            context("프로젝트에 접근 권한이 없는 경우") {
                every { exceptionTestService.error() } throws NonAccessibleProjectException(1L)

                val code = NON_ACCESSIBLE_PROJECT.code()
                it("NON_ACCESSIBLE_PROJECT: 에러코드 $code") {
                    mockMvc.perform(get("/error")
                    ).andDo(document(
                        "error/project/2",
                        getDocumentResponse())
                    ).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf PROJECT
                }
            }
        }

        this.describe("Page 도메인") {
            PAGE.code shouldBe 200

            context("페이지가 삭제되거나 존재하지 않을 때") {
                every { exceptionTestService.error() } throws ProjectPageNotFoundException(1L)

                val code = PROJECT_PAGE_NOT_FOUND.code()
                it("PROJECT_PAGE_NOT_FOUND: 에러코드 $code") {
                    mockMvc.perform(get("/error")
                    ).andDo(document(
                        "error/page/1",
                        getDocumentResponse())
                    ).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf PAGE
                }
            }

            context("페이지에 접근 권한이 없는 경우") {
                every { exceptionTestService.error() } throws NonAccessibleProjectPageException(1L)

                val code = NON_ACCESSIBLE_PROJECT_PAGE.code()
                it("NON_ACCESSIBLE_PROJECT_PAGE: 에러코드 $code") {
                    mockMvc.perform(get("/error")
                    ).andDo(document(
                        "error/page/2",
                        getDocumentResponse())
                    ).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf PAGE
                }
            }
        }

        this.describe("Object 도메인") {
            OBJECT.code shouldBe 300

            context("오브젝트가 삭제되거나 존재하지 않을 때") {
                every { exceptionTestService.error() } throws PageObjectNotFoundException(1L)

                val code = PAGE_OBJECT_NOT_FOUND.code()
                it("PAGE_OBJECT_NOT_FOUND: 에러코드 $code") {
                    mockMvc.perform(get("/error")
                    ).andDo(document(
                        "error/object/1",
                        getDocumentResponse())
                    ).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf OBJECT
                }
            }

            context("오브젝트에 접근 권한이 없는 경우") {
                every { exceptionTestService.error() } throws NonAccessiblePageObjectException(1L)

                val code = NON_ACCESSIBLE_PAGE_OBJECT.code()
                it("NON_ACCESSIBLE_PAGE_OBJECT: 에러코드 $code") {
                    mockMvc.perform(get("/error")
                    ).andDo(document(
                        "error/object/2",
                        getDocumentResponse())
                    ).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf OBJECT
                }
            }
        }

        this.describe("Event 도메인: 400번대") {
            EVENT.code shouldBe 400

            context("이벤트가 삭제되거나 존재하지 않을 때") {
                every { exceptionTestService.error() } throws ObjectEventNotFoundException(1L)

                val code = OBJECT_EVENT_NOT_FOUND.code()
                it("OBJECT_EVENT_NOT_FOUND: 에러코드 $code") {
                    mockMvc.perform(get("/error")
                    ).andDo(document(
                        "error/event/1",
                        getDocumentResponse())
                    ).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf EVENT
                }
            }

            context("이벤트에 접근 권한이 없는 경우") {
                every { exceptionTestService.error() } throws NonAccessibleObjectEventException(1L)

                val code = NON_ACCESSIBLE_OBJECT_EVENT.code()
                it("NON_ACCESSIBLE_OBJECT_EVENT: 에러코드 $code") {
                    mockMvc.perform(get("/error")
                    ).andDo(document(
                        "error/event/2",
                        getDocumentResponse())
                    ).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf EVENT
                }
            }

            context("이벤트를 생성할 때, 기존 오브젝트에 이미 이벤트가 할당되어 있는 경우") {
                every { exceptionTestService.error() } throws MultipleEventAllocationException(1L)

                val code = ONLY_SINGLE_EVENT_PER_OBJECT.code()
                it("ONLY_SINGLE_EVENT_PER_OBJECT: 에러코드 $code") {
                    mockMvc.perform(get("/error")
                    ).andDo(document(
                        "error/event/3",
                        getDocumentResponse())
                    ).andExpect(status().isConflict
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf EVENT
                }
            }

            context("이벤트의 다음 페이지를 설정할 때, 해당 페이지가 같은 프로젝트 내 없는 경우") {
                every { exceptionTestService.error() } throws LinkNonRelatedPageException(1L)

                val code = PAGE_IN_OTHER_PROJECT.code()
                it("PAGE_IN_OTHER_PROJECT: 에러코드 $code") {
                    mockMvc.perform(get("/error")
                    ).andDo(document(
                        "error/event/4",
                        getDocumentResponse())
                    ).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(code)))

                    code shouldBeDomainOf EVENT
                }
            }
        }
    }
}