package com.wafflestudio.webgam.domain.event

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.wafflestudio.webgam.ENUM
import com.wafflestudio.webgam.NUMBER
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentRequest
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentResponse
import com.wafflestudio.webgam.RestDocsUtils.Companion.requestBody
import com.wafflestudio.webgam.TestUtils
import com.wafflestudio.webgam.domain.event.model.TransitionType
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import com.wafflestudio.webgam.type
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ActiveProfiles("test")
@DisplayName("Event 통합 테스트")
class EventDescribeSpec(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
): DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    override suspend fun beforeSpec(spec: Spec) {
        userRepository.saveAll(data)
    }

    override suspend fun afterSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            userRepository.deleteAll()
        }
    }

    companion object {
        private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        private val data = TestUtils.docTestData()
        private val auth = WebgamAuthenticationToken(UserPrincipal(data.first()), "")
        private val project = data.first().projects.first()
        private val page = project.pages.first()
        private val pageObject = page.objects.first { it.event == null }
        private val event = page.objects.first().event!!
        private val nextPage = project.pages.first()
    }

    init {
        this.describe("이벤트 조회할 때") {
            context("성공적인 경우") {
                it("200 OK") {
                    mockMvc.perform(
                        get("/api/v1/events/{id}", event.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "event/get",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        pathParameters(parameterWithName("id").description("이벤트 ID"))
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("이벤트 생성할 때") {
            context("성공적인 경우") {
                val request = mapOf(
                    "object_id" to pageObject.id,
                    "transition_type" to TransitionType.DEFAULT,
                    "page_id" to page.id
                )

                it("200 OK") {
                    mockMvc.perform(
                        post("/api/v1/events")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "event/post",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestBody(
                            "object_id" type NUMBER means "오브젝트 ID" formattedAs "0 초과",
                            "transition_type" type ENUM(TransitionType::class) means "전환 효과" withDefaultValue "DEFAULT" isOptional true,
                            "page_id" type NUMBER means "다음 페이지 ID" formattedAs "0 초과" isOptional true,
                        )
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("이벤트 수정할 때") {
            context("성공적인 경우") {
                val request = mapOf(
                    "page_id" to nextPage.id
                )

                it("200 OK") {
                    mockMvc.perform(
                        patch("/api/v1/events/{id}", event.id.toString())
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "event/patch",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        pathParameters(parameterWithName("id").description("이벤트 ID")),
                        requestBody(
                            "transition_type" type ENUM(TransitionType::class) means "전환 효과" withDefaultValue "DEFAULT" isOptional true,
                            "page_id" type NUMBER means "다음 페이지 ID" formattedAs "0 초과" isOptional true,
                        )
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("이벤트 삭제할 때") {
            context("성공적인 경우") {
                it("200 OK") {
                    mockMvc.perform(
                        delete("/api/v1/events/{id}", event.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "event/delete",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        pathParameters(parameterWithName("id").description("이벤트 ID")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }
    }
}