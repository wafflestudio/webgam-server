package com.wafflestudio.webgam.domain.page

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.wafflestudio.webgam.*
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentRequest
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentResponse
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
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
@DisplayName("Page 통합 테스트")
class PageDescribeSpec (
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
): DescribeSpec(){

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
    }

    init{
        this.describe("프로젝트 페이지 조회할 때") {
            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(get("/api/v1/pages/{id}", page.id)
                            .with(authentication(auth))
                    ).andDo(document(
                            "page/get",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            pathParameters(parameterWithName("id").description("프로젝트 페이지 ID")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("프로젝트 페이지 생성할 때") {
            context("성공하면") {
                val request = mapOf("project_id" to project.id, "name" to "create-page")

                it("200 OK") {
                    mockMvc.perform(post("/api/v1/pages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                            "page/create",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            RestDocsUtils.requestBody(
                                    "project_id" type NUMBER means "프로젝트 ID",
                                    "name" type STRING means "페이지 이름"
                            )
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("프로젝트 페이지 수정할 때") {
            context("성공하면") {
                val request = mapOf("name" to "patch-page")

                it("200 OK") {
                    mockMvc.perform(patch("/api/v1/pages/{id}", page.id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                            "page/patch",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            pathParameters(parameterWithName("id").description("프로젝트 페이지 ID")),
                            RestDocsUtils.requestBody(
                                    "name" type STRING means "페이지 이름" isOptional true
                            )
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("프로젝트 페이지 삭제할 때") {
            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(
                            delete("/api/v1/pages/{id}", page.id.toString())
                                    .with(authentication(auth))
                    ).andDo(document(
                            "page/delete",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            pathParameters(parameterWithName("id").description("프로젝트 페이지 ID")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }
    }
}