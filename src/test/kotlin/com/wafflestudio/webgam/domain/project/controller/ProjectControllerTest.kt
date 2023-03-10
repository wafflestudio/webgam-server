package com.wafflestudio.webgam.domain.project.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.ninjasquad.springmockk.MockkBean
import com.wafflestudio.webgam.TestUtils
import com.wafflestudio.webgam.TestUtils.Companion.pathVariableIds
import com.wafflestudio.webgam.domain.project.dto.ProjectDto.DetailedResponse
import com.wafflestudio.webgam.domain.project.dto.ProjectDto.SimpleResponse
import com.wafflestudio.webgam.domain.project.service.ProjectService
import com.wafflestudio.webgam.global.common.exception.ErrorType
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
import org.springframework.data.domain.SliceImpl
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.*

@WebMvcTest(ProjectController::class)
@MockkBean(JpaMetamodelMappingContext::class)
@ActiveProfiles("test")
@DisplayName("ProjectController 테스트")
class ProjectControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean private val projectService: ProjectService,
): DescribeSpec() {

    companion object {
        private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        private val user = TestUtils.testData1().first()
        private val authentication = WebgamAuthenticationToken(UserPrincipal(user), "")
        private val project = user.projects.first()

        /* Test Parameters */
        private val ids = pathVariableIds()
    }

    override suspend fun beforeSpec(spec: Spec) {
        every { projectService.getProjectList(any(), any()) } returns SliceImpl(listOf(SimpleResponse(project)))
        every { projectService.createProject(any(), any()) } returns DetailedResponse(project)
        every { projectService.getProject(any(), any()) } returns DetailedResponse(project)
        every { projectService.patchProject(any(), any(), any()) } returns DetailedResponse(project)
        justRun { projectService.deleteProject(any(), any()) }
    }

    init {
        this.describe("프로젝트 생성할 때") {
            val titles = listOf(
                listOf("valid-project-title").map { it to null },
                listOf(null, "", "   ").map { it to ErrorType.BadRequest.INVALID_FIELD.code() }).flatten()

            val combinations = TestUtils.makeFieldList(titles)

            val fields = listOf("title")

            combinations.forAll { (l, t) ->
                val (idx, code) = t
                val request = l.mapIndexed { index, field -> fields[index] to field }.toMap()

                when (idx) {
                    -1 -> { context("Body의 모든 field가 올바르면") {
                        it("200 OK") {
                            mockMvc.post("/api/v1/projects") {
                                contentType = MediaType.APPLICATION_JSON
                                content = gson.toJson(request)
                                with(authentication(authentication))
                                with(csrf())
                            }.andExpect { status { isOk() } }
                        }
                    } }
                    else -> { context("${fields[idx]}가 올바르지 않은 값 '${l[idx]}' 이면") {
                        it ("400 Bad Request, 에러코드 $code") {
                            mockMvc.post("/api/v1/projects") {
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

        this.describe("특정 프로젝트 조회할 때") {
            val ids = TestUtils.makeFieldList(ids)

            ids.forAll { (i, t) ->
                val (idx, code) = t
                val id = i[0].toString()

                if (idx == -1) context("ID가 올바른 값 '${id}'이면") {
                    it("200 OK") {
                        mockMvc.get("/api/v1/projects/{id}", id) {
                            param("project-id", id)
                            with(authentication(authentication))
                        }.andExpect { status { isOk() } }
                    }
                }
                else context("ID가 올바르지 않은 값 '${id}'이면") {
                    it("400 Bad Request, 에러코드 $code") {
                        mockMvc.get("/api/v1/projects/{id}", id) {
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

        this.describe("특정 프로젝트 수정할 때") {
            val titles = listOf(
                listOf("valid-page-name").map { it to null },
                listOf(null, "", "   ").map { it to ErrorType.BadRequest.INVALID_FIELD.code() }).flatten()

            val combinations = TestUtils.makeFieldList(ids, titles)

            val fields = listOf("id", "title")

            combinations.forAll { (l, t) ->
                val (idx, code) = t
                val request = mapOf(
                    "title" to l[1]
                )

                when (idx) {
                    -1 -> { context("Body의 모든 field가 올바르면") {
                        it("200 OK") {
                            mockMvc.patch("/api/v1/projects/{id}", l[0]) {
                                contentType = MediaType.APPLICATION_JSON
                                content = gson.toJson(request)
                                with(authentication(authentication))
                                with(csrf())
                            }.andExpect { status { isOk() } }
                        }
                    } }
                    else -> { context("${fields[idx]}가 올바르지 않은 값 '${l[idx]}' 이면") {
                        it ("400 Bad Request, 에러코드 $code") {
                            mockMvc.patch("/api/v1/projects/{id}", l[0]) {
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

        this.describe("특정 프로젝트 삭제할 때") {
            val ids = TestUtils.makeFieldList(ids)

            ids.forAll { (i, t) ->
                val (idx, code) = t
                val id = i[0].toString()

                if (idx == -1) context("ID가 올바른 값 '${id}'이면") {
                    it("200 OK") {
                        mockMvc.delete("/api/v1/projects/{id}", id) {
                            with(authentication(authentication))
                            with(csrf())
                        }.andExpect { status { isOk() } }
                    }
                }
                else context("ID가 올바르지 않은 값 '${id}'이면") {
                    it("400 Bad Request, 에러코드 $code") {
                        mockMvc.delete("/api/v1/projects/{id}", id) {
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

        this.describe("모든 프로젝트 목록 조회할 때") {
            val pages = listOf(
                listOf(0, 1, 3, 4, 10, null).map { it to null },
                listOf(-1).map { it to ErrorType.BadRequest.CONSTRAINT_VIOLATION.code() },
                listOf("nonInt").map { it to ErrorType.BadRequest.PARAMETER_TYPE_MISMATCH.code() }).flatten()
            val sizes = listOf(
                listOf(1, 3, 4, 10, null).map { it to null },
                listOf(-1, 0).map { it to ErrorType.BadRequest.CONSTRAINT_VIOLATION.code() },
                listOf("nonInt").map { it to ErrorType.BadRequest.PARAMETER_TYPE_MISMATCH.code() }).flatten()

            val combinations = TestUtils.makeFieldList(pages, sizes)

            val fields = listOf("page", "size")

            combinations.forAll { (l, t) ->
                val (idx, code) = t

                when (idx) {
                    -1 -> { context("모든 query parameter가 올바르면") {
                        it("200 OK") {
                            mockMvc.get("/api/v1/projects") {
                                l[0] ?.let { param("page", l[0].toString()) }
                                l[1] ?.let { param("size", l[1].toString()) }
                                with(authentication(authentication))
                                with(csrf())
                            }.andExpect { status { isOk() } }
                        }
                    } }
                    else -> { context("${fields[idx]}가 올바르지 않은 값 '${l[idx]}' 이면") {
                        it ("400 Bad Request, 에러코드 $code") {
                            mockMvc.get("/api/v1/projects") {
                                l[0] ?.let { param("page", l[0].toString()) }
                                l[1] ?.let { param("size", l[1].toString()) }
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
    }
}