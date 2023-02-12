package com.wafflestudio.webgam.domain.event.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.ninjasquad.springmockk.MockkBean
import com.wafflestudio.webgam.TestUtils
import com.wafflestudio.webgam.domain.event.dto.ObjectEventDto
import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import com.wafflestudio.webgam.domain.event.model.TransitionType
import com.wafflestudio.webgam.domain.event.service.ObjectEventService
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.*
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.inspectors.forAll
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
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

@WebMvcTest(ObjectEventController::class)
@MockkBean(JpaMetamodelMappingContext::class)
@ActiveProfiles("test")
@Tag("Unit-Test")
@DisplayName("ObjectEventController 단위 테스트")
class ObjectEventControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @MockkBean private val objectEventService: ObjectEventService,
): DescribeSpec() {

    companion object {
        private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        private val user = User("", "", "", "")
        private val authentication = WebgamAuthenticationToken(UserPrincipal(user), "")
        private val pageObject = mockk<PageObject>()
        private val event = ObjectEvent(pageObject, null, TransitionType.DEFAULT)

        /* Test Parameters */
        private val ids = listOf(
            listOf("1", "3", "5", "100").map { it to null },
            listOf("0", "-1", "-100").map { it to CONSTRAINT_VIOLATION.code() }).flatten()
    }

    override suspend fun beforeSpec(spec: Spec) {
        every { objectEventService.createEvent(any(), any()) } returns ObjectEventDto.SimpleResponse(event)
        every { objectEventService.getEvent(any(), any()) } returns ObjectEventDto.SimpleResponse(event)
        every { objectEventService.updateEvent(any(), any(), any()) } returns ObjectEventDto.SimpleResponse(event)
        justRun { objectEventService.deleteEvent(any(), any()) }
    }

    init {
        this.describe("이벤트를 생성할 때") {

            /* Test Parameters */
            val objectIds = listOf(
                listOf(1, 3, 4, 10).map { it to null },
                listOf(-1, 0, null).map { it to INVALID_FIELD.code() },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val transitionTypes = listOf(
                listOf("DEFAULT", null).map { it to null },
                listOf("nonEnum", "default").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val nextPageIds = listOf(
                listOf(1, 3, 4, 10, null).map { it to null },
                listOf(-1, 0).map { it to INVALID_FIELD.code() },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()

            val combinations = TestUtils.makeFieldList(objectIds, transitionTypes, nextPageIds)
            val fields = listOf("object_id", "transition_type", "next_page_id")

            combinations.forAll { (l, t) ->
                val (idx, code) = t
                val request = l.mapIndexed { index, field -> fields[index] to field }.toMap()

                when (idx) {
                    -1 -> { context("Body의 모든 field가 올바르면") {
                        it("200 OK") {
                            mockMvc.post("/api/v1/events") {
                                contentType = MediaType.APPLICATION_JSON
                                content = gson.toJson(request)
                                with(authentication(authentication))
                                with(csrf())
                            }.andDo { print() }.andExpect { status { isOk() } }
                        }
                    } }
                    else -> { context("${fields[idx]}가 올바르지 않은 값 '${l[idx]}' 이면") {
                        it ("400 Bad Request, 에러코드 $code") {
                            mockMvc.post("/api/v1/events") {
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

        this.describe("이벤트를 조회할 때") {
            val ids = TestUtils.makeFieldList(ids)

            ids.forAll { (i, t) ->
                val (idx, code) = t
                val id = i[0].toString()

                if (idx == -1) context("ID가 올바른 값 '${id}'이면") {
                    it("200 OK") {
                        mockMvc.get("/api/v1/events/{id}", id) {
                            with(authentication(authentication))
                        }.andExpect { status { isOk() } }
                    }
                }
                else context("ID가 올바르지 않은 값 '${id}'이면") {
                    it("400 Bad Request, 에러코드 $code") {
                        mockMvc.get("/api/v1/events/{id}", id) {
                            with(authentication(authentication))
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", Is.`is`(code))
                        }.andDo { print() }
                    }
                }
            }
        }

        this.describe("이벤트를 수정할 때") {

            /* Test Parameters */
            val transitionTypes = listOf(
                listOf("DEFAULT", null).map { it to null },
                listOf("nonEnum", "default").map { it to JSON_PARSE_ERROR.code() }).flatten()
            val nextPageIds = listOf(
                listOf(1, 3, 4, 10, null).map { it to null },
                listOf(-1, 0).map { it to INVALID_FIELD.code() },
                listOf("nonInt").map { it to JSON_PARSE_ERROR.code() }).flatten()

            val combinations = TestUtils.makeFieldList(ids, transitionTypes, nextPageIds)
            val fields = listOf("ignored", "transition_type", "next_page_id")

            combinations.forAll { (l, t) ->
                val (idx, code) = t
                val id = l[0].toString()
                val request = l.mapIndexed { index, field -> fields[index] to field }.toMap()

                when (idx) {
                    -1 -> { context("Body의 모든 field가 올바르면") {
                        it("200 OK") {
                            mockMvc.patch("/api/v1/events/{id}", id) {
                                contentType = MediaType.APPLICATION_JSON
                                content = gson.toJson(request)
                                with(authentication(authentication))
                                with(csrf())
                            }.andDo { print() }.andExpect { status { isOk() } }
                        }
                    } }
                    else -> { context("${fields[idx]}가 올바르지 않은 값 '${l[idx]}' 이면") {
                        it ("400 Bad Request, 에러코드 $code") {
                            mockMvc.patch("/api/v1/events/{id}", id) {
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

        this.describe("이벤트를 삭제할 때") {
            val ids = TestUtils.makeFieldList(ids)

            ids.forAll { (i, t) ->
                val (idx, code) = t
                val id = i[0].toString()

                if (idx == -1) context("ID가 올바른 값 '${id}'이면") {
                    it("200 OK") {
                        mockMvc.delete("/api/v1/events/{id}", id) {
                            with(authentication(authentication))
                            with(csrf())
                        }.andExpect { status { isOk() } }
                    }
                }
                else context("ID가 올바르지 않은 값 '${id}'이면") {
                    it("400 Bad Request, 에러코드 $code") {
                        mockMvc.delete("/api/v1/events/{id}", id) {
                            with(authentication(authentication))
                            with(csrf())
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.error_code", Is.`is`(code))
                        }.andDo { print() }
                    }
                }
            }
        }
    }
}