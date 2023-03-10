package com.wafflestudio.webgam.domain.`object`.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.ninjasquad.springmockk.MockkBean
import com.wafflestudio.webgam.TestUtils
import com.wafflestudio.webgam.TestUtils.Companion.pathVariableIds
import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto.DetailedResponse
import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto.SimpleResponse
import com.wafflestudio.webgam.domain.`object`.service.PageObjectService
import com.wafflestudio.webgam.global.common.dto.ListResponse
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.*
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.inspectors.forAll
import io.mockk.every
import io.mockk.justRun
import org.hamcrest.core.Is.`is`
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.*

@WebMvcTest(PageObjectController::class)
@MockkBean(JpaMetamodelMappingContext::class)
@ActiveProfiles("test")
@DisplayName("PageObjectController 단위 테스트")
class PageObjectControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean private val pageObjectService: PageObjectService,
): DescribeSpec() {

    companion object {
        private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        private val user = TestUtils.testData1().first()
        private val authentication = WebgamAuthenticationToken(UserPrincipal(user), "")
        private val pageObject = user.projects.first().pages.first { it.objects.isNotEmpty() }.objects.first()

        /* Test Parameters */
        private val ids = pathVariableIds()
    }

    override suspend fun beforeSpec(spec: Spec) {
        every { pageObjectService.listProjectObjects(any(), any()) } returns ListResponse(listOf(DetailedResponse(pageObject)))
        every { pageObjectService.createObject(any(), any()) } returns SimpleResponse(pageObject)
        every { pageObjectService.getObject(any(), any()) } returns DetailedResponse(pageObject)
        every { pageObjectService.modifyObject(any(), any(), any()) } returns DetailedResponse(pageObject)
        justRun { pageObjectService.deleteObject(any(), any()) }
    }

    init {
        this.describe("특정 프로젝트의 모든 오브젝트를 조회할 때") {
            val ids = TestUtils.makeFieldList(ids)

            ids.forAll { (i, t) ->
                val (idx, code) = t
                val id = i[0].toString()

                if (idx == -1) context("ID가 올바른 값 '${id}'이면") {
                    it("200 OK") {
                        mockMvc.get("/api/v1/objects") {
                            param("project-id", id)
                            with(authentication(authentication))
                        }.andExpect { status { isOk() } }
                    }
                }
                else context("ID가 올바르지 않은 값 '${id}'이면") {
                    it("400 Bad Request, 에러코드 $code") {
                        mockMvc.get("/api/v1/objects") {
                            param("project-id", id)
                            with(authentication(authentication))
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", `is`(code))
                        }.andDo { print() }
                    }
                }
            }
        }

        this.describe("오브젝트 생성할 때") {
            val pageIds = listOf(
                listOf(1, 3, 4, 10).map { it to null },
                listOf(-1, 0, null).map { it to INVALID_FIELD.code() },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val names = listOf(
                listOf("valid-object-name").map { it to null },
                listOf(null).map { it to INVALID_FIELD.code() }).flatten()
            val objectTypes = listOf(
                listOf("DEFAULT", "TEXT", "IMAGE", null).map { it to null },
                listOf("not-enum", "default", "text", "image").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val widths = listOf(
                listOf(1, 3).map { it to null },
                listOf(-1, 0, null).map { it to INVALID_FIELD.code() },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val heights = listOf(
                listOf(1, 3).map { it to null },
                listOf(-1, 0, null).map { it to INVALID_FIELD.code() },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val xPositions = listOf(
                listOf(1, -1, 0).map { it to null },
                listOf("").map { it to INVALID_FIELD.code() },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val yPositions = listOf(
                listOf(1, -1, 0).map { it to null },
                listOf("").map { it to INVALID_FIELD.code() },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val zIndices = listOf(
                listOf(0, 1, 3).map { it to null },
                listOf(-1, null).map { it to INVALID_FIELD.code() },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val textContents = listOf(
                listOf("valid text content", null).map { it to null },
                ).flatten()
            val fontSizes = listOf(
                listOf(1, null).map { it to null },
                listOf(0, -1).map { it to INVALID_FIELD.code() }).flatten()
            val imageSources = listOf(
                listOf("http://valid-url.com", "https://secure-url", null).map { it to null },
                listOf("something", "with-no-domain").map { it to INVALID_FIELD.code() }).flatten()

            val combinations = TestUtils.makeFieldList(
                pageIds,
                names,
                objectTypes,
                widths,
                heights,
                xPositions,
                yPositions,
                zIndices,
                textContents,
                fontSizes,
                imageSources
            )

            val fields = listOf("page_id", "name", "type", "width", "height", "x_position", "y_position",
                "z_index", "text_content", "font_size", "image_source")

            combinations.forAll { (l, t) ->
                val (idx, code) = t
                val request = l.mapIndexed { index, field -> fields[index] to field }.toMap()

                when (idx) {
                    -1 -> { context("Body의 모든 field가 올바르면") {
                        it("200 OK") {
                            mockMvc.post("/api/v1/objects") {
                                contentType = MediaType.APPLICATION_JSON
                                content = gson.toJson(request)
                                with(authentication(authentication))
                                with(csrf())
                            }.andExpect { status { isOk() } }
                        }
                    } }
                    else -> { context("${fields[idx]}가 올바르지 않은 값 '${l[idx]}' 이면") {
                        it ("400 Bad Request, 에러코드 $code") {
                            mockMvc.post("/api/v1/objects") {
                                contentType = MediaType.APPLICATION_JSON
                                content = gson.toJson(request)
                                with(authentication(authentication))
                                with(csrf())
                            }.andExpect {
                                status { isBadRequest() }
                                jsonPath("$.error_code", `is`(code))
                            }
                        }
                    } }
                }
            }
        }

        this.describe("특정 오브젝트 조회할 때") {
            val ids = TestUtils.makeFieldList(ids)

            ids.forAll { (i, t) ->
                val (idx, code) = t
                val id = i[0].toString()

                if (idx == -1) context("ID가 올바른 값 '${id}'이면") {
                    it("200 OK") {
                        mockMvc.get("/api/v1/objects/{id}", id) {
                            with(authentication(authentication))
                        }.andExpect { status { isOk() } }
                    }
                }
                else context("ID가 올바르지 않은 값 '${id}'이면") {
                    it("400 Bad Request, 에러코드 $code") {
                        mockMvc.get("/api/v1/objects/{id}", id) {
                            with(authentication(authentication))
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", `is`(code))
                        }
                    }
                }
            }



        }

        this.describe("특정 오브젝트 수정할 때") {
            val objectTypes = listOf(
                listOf("DEFAULT", "TEXT", "IMAGE", "else-falls-to-default", null).map { it to null },
            ).flatten()
            val widths = listOf(
                listOf(1, 3, null).map { it to null },
                listOf(-1, 0).map { it to INVALID_FIELD.code() },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val heights = listOf(
                listOf(1, 3, null, "").map { it to null },
                listOf(-1, 0).map { it to INVALID_FIELD.code() },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val xPositions = listOf(
                listOf(1, -1, 0, null, "").map { it to null },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val yPositions = listOf(
                listOf(1, -1, 0, null, "").map { it to null },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val zIndices = listOf(
                listOf(0, 1, 3, null).map { it to null },
                listOf(-1).map { it to INVALID_FIELD.code() },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val textContents = listOf(
                listOf("valid text content", null).map { it to null },
            ).flatten()
            val fontSizes = listOf(
                listOf(1, null).map { it to null },
                listOf(0, -1).map { it to INVALID_FIELD.code() }).flatten()
            val imageSources = listOf(
                listOf("http://valid-url.com", "https://secure-url", null).map { it to null },
                listOf("something", "with-no-domain").map { it to INVALID_FIELD.code() }).flatten()

            val combinations = TestUtils.makeFieldList(
                ids,
                objectTypes,
                widths,
                heights,
                xPositions,
                yPositions,
                zIndices,
                textContents,
                fontSizes,
                imageSources
            )

            val fields = listOf("id", "object_type", "width", "height", "x_position", "y_position", "z_index", "text_content", "font_size", "image_source")

            combinations.forAll { (l, t) ->
                val (idx, code) = t
                val request = mapOf(
                    "object_type" to l[1],
                    "width" to l[2],
                    "height" to l[3],
                    "x_position" to l[4],
                    "y_position" to l[5],
                    "z_index" to l[6],
                    "text_content" to l[7],
                    "font_size" to l[8],
                    "image_source" to l[9],
                )

                when (idx) {
                    -1 -> {
                        context("Body의 모든 field가 올바르면") {
                            it("200 OK") {
                                mockMvc.patch("/api/v1/objects/{id}", l[0]) {
                                    contentType = MediaType.APPLICATION_JSON
                                    content = gson.toJson(request)
                                    with(authentication(authentication))
                                    with(csrf())
                                }.andExpect { status { isOk() } }
                            }
                        }
                    }
                    else -> {
                        context("${fields[idx]}가 올바르지 않은 값 '${l[idx]}' 이면") {
                            it("400 Bad Request, 에러코드 $code") {
                                mockMvc.patch("/api/v1/objects/{id}", l[0]) {
                                    contentType = MediaType.APPLICATION_JSON
                                    content = gson.toJson(request)
                                    with(authentication(authentication))
                                    with(csrf())
                                }.andExpect {
                                    status { isBadRequest() }
                                    jsonPath("$.error_code", `is`(code))
                                }
                            }
                        }
                    }
                }
            }
        }

        this.describe("특정 오브젝트 삭제할 때") {
            val ids = TestUtils.makeFieldList(ids)

            ids.forAll { (i, t) ->
                val (idx, code) = t
                val id = i[0].toString()

                if (idx == -1) context("ID가 올바른 값 '${id}'이면") {
                    it("200 OK") {
                        mockMvc.delete("/api/v1/objects/{id}", id) {
                            with(authentication(authentication))
                            with(csrf())
                        }.andExpect { status { isOk() } }
                    }
                }
                else context("ID가 올바르지 않은 값 '${id}'이면") {
                    it("400 Bad Request, 에러코드 $code") {
                        mockMvc.delete("/api/v1/objects/{id}", id) {
                            with(authentication(authentication))
                            with(csrf())
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", `is`(code))
                        }
                    }
                }
            }
        }
    }
}
