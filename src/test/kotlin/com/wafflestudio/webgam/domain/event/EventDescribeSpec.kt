package com.wafflestudio.webgam.domain.event

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.wafflestudio.webgam.ENUM
import com.wafflestudio.webgam.NUMBER
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentRequest
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentResponse
import com.wafflestudio.webgam.RestDocsUtils.Companion.requestBody
import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import com.wafflestudio.webgam.domain.event.model.TransitionType
import com.wafflestudio.webgam.domain.event.repository.ObjectEventRepository
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType
import com.wafflestudio.webgam.domain.`object`.repository.PageObjectRepository
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import com.wafflestudio.webgam.type
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Transactional
@ActiveProfiles("test")
@Tag("Integration-Test")
@DisplayName("Event 통합 테스트")
class EventDescribeSpec(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val projectRepository: ProjectRepository,
    @Autowired private val projectPageRepository: ProjectPageRepository,
    @Autowired private val pageObjectRepository: PageObjectRepository,
    @Autowired private val objectEventRepository: ObjectEventRepository,
): DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    private final val user = userRepository.save(User("fooId", "foo", "foo@wafflestudio.com", ""))
    private final val auth = WebgamAuthenticationToken(UserPrincipal(user), "")
    private final val project = projectRepository.save(Project(user, "test-project"))
    private final val page = projectPageRepository.save(ProjectPage(project, "test-page"))
    private final val nextPage = projectPageRepository.save(ProjectPage(project, "sample-next-page"))
    private final val pageObject = pageObjectRepository.save(PageObject(page, "test-object", PageObjectType.DEFAULT,
            10, 10, 0, 0, 0, null, null, null, null))
    private final val event = objectEventRepository.save(ObjectEvent(pageObject, null, TransitionType.DEFAULT))

    companion object {
        private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
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