package com.wafflestudio.webgam.domain.page.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.ninjasquad.springmockk.MockkBean
import com.wafflestudio.webgam.TestUtils
import com.wafflestudio.webgam.TestUtils.Companion.pathVariableIds
import com.wafflestudio.webgam.TestUtils.Companion.testData1
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto.DetailedResponse
import com.wafflestudio.webgam.domain.page.service.ProjectPageService
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.INVALID_FIELD
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.JSON_PARSE_ERROR
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.inspectors.forAll
import io.mockk.every
import io.mockk.justRun
import org.hamcrest.core.Is
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.*

@WebMvcTest(ProjectPageController::class)
@MockkBean(JpaMetamodelMappingContext::class)
@ActiveProfiles("test")
@DisplayName("ProjectPageController 테스트")
class ProjectPageControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean private val projectPageService: ProjectPageService,
): DescribeSpec() {

    companion object {
        private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        private val user = testData1().first()
        private val authentication = WebgamAuthenticationToken(UserPrincipal(user), "")
        private val page = user.projects.first().pages.first()

        /* Test Parameters */
        private val ids = pathVariableIds()
    }

    override suspend fun beforeSpec(spec: Spec) {
        every { projectPageService.createProjectPage(any(), any()) } returns DetailedResponse(page)
        every { projectPageService.getProjectPage(any(), any()) } returns DetailedResponse(page)
        every { projectPageService.patchProjectPage(any(), any(), any()) } returns DetailedResponse(page)
        justRun { projectPageService.deleteProjectPage(any(), any()) }
    }

    init {
        this.describe("페이지 생성할 때") {
            val projectIds = listOf(
                listOf(1, 3, 4, 10).map { it to null },
                listOf(-1, 0, null).map { it to INVALID_FIELD.code() },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val names = listOf(
                listOf("valid-page-name").map { it to null },
                listOf(null, "", "   ").map { it to INVALID_FIELD.code() }).flatten()

            val combinations = TestUtils.makeFieldList(projectIds, names)

            val fields = listOf("project_id", "name")

            combinations.forAll { (l, t) ->
                val (idx, code) = t
                val request = l.mapIndexed { index, field -> fields[index] to field }.toMap()

                when (idx) {
                    -1 -> { context("Body의 모든 field가 올바르면") {
                        it("200 OK") {
                            mockMvc.post("/api/v1/pages") {
                                contentType = MediaType.APPLICATION_JSON
                                content = gson.toJson(request)
                                with(authentication(authentication))
                                with(csrf())
                            }.andExpect { status { isOk() } }
                        }
                    } }
                    else -> { context("${fields[idx]}가 올바르지 않은 값 '${l[idx]}' 이면") {
                        it ("400 Bad Request, 에러코드 $code") {
                            mockMvc.post("/api/v1/pages") {
                                contentType = MediaType.APPLICATION_JSON
                                content = gson.toJson(request)
                                with(authentication(authentication))
                                with(csrf())
                            }.andExpect {
                                status { isBadRequest() }
                                jsonPath("$.error_code", Is.`is`(code))
                            }
                        }
                    } }
                }
            }
        }

        this.describe("특정 페이지 조회할 때") {
            val ids = TestUtils.makeFieldList(ids)

            ids.forAll { (i, t) ->
                val (idx, code) = t
                val id = i[0].toString()

                if (idx == -1) context("ID가 올바른 값 '${id}'이면") {
                    it("200 OK") {
                        mockMvc.get("/api/v1/pages/{id}", id) {
                            param("project-id", id)
                            with(authentication(authentication))
                        }.andExpect { status { isOk() } }
                    }
                }
                else context("ID가 올바르지 않은 값 '${id}'이면") {
                    it("400 Bad Request, 에러코드 $code") {
                        mockMvc.get("/api/v1/pages/{id}", id) {
                            param("project-id", id)
                            with(authentication(authentication))
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", Is.`is`(code))
                        }
                    }
                }
            }
        }

        this.describe("특정 페이지 수정할 때") {
            val names = listOf(
                listOf("valid-page-name").map { it to null },
                listOf(null, "", "   ").map { it to INVALID_FIELD.code() }).flatten()

            val combinations = TestUtils.makeFieldList(ids, names)

            val fields = listOf("id", "name")

            combinations.forAll { (l, t) ->
                val (idx, code) = t
                val request = mapOf(
                    "name" to l[1]
                )

                when (idx) {
                    -1 -> { context("Body의 모든 field가 올바르면") {
                        it("200 OK") {
                            mockMvc.patch("/api/v1/pages/{id}", l[0]) {
                                contentType = MediaType.APPLICATION_JSON
                                content = gson.toJson(request)
                                with(authentication(authentication))
                                with(csrf())
                            }.andExpect { status { isOk() } }
                        }
                    } }
                    else -> { context("${fields[idx]}가 올바르지 않은 값 '${l[idx]}' 이면") {
                        it ("400 Bad Request, 에러코드 $code") {
                            mockMvc.patch("/api/v1/pages/{id}", l[0]) {
                                contentType = MediaType.APPLICATION_JSON
                                content = gson.toJson(request)
                                with(authentication(authentication))
                                with(csrf())
                            }.andExpect {
                                status { isBadRequest() }
                                jsonPath("$.error_code", Is.`is`(code))
                            }
                        }
                    } }
                }
            }
        }

        this.describe("특정 페이지 삭제할 때") {
            val ids = TestUtils.makeFieldList(ids)

            ids.forAll { (i, t) ->
                val (idx, code) = t
                val id = i[0].toString()

                if (idx == -1) context("ID가 올바른 값 '${id}'이면") {
                    it("200 OK") {
                        mockMvc.delete("/api/v1/pages/{id}", id) {
                            with(authentication(authentication))
                            with(csrf())
                        }.andExpect { status { isOk() } }
                    }
                }
                else context("ID가 올바르지 않은 값 '${id}'이면") {
                    it("400 Bad Request, 에러코드 $code") {
                        mockMvc.delete("/api/v1/pages/{id}", id) {
                            with(authentication(authentication))
                            with(csrf())
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", Is.`is`(code))
                        }
                    }
                }
            }
        }
    }
}