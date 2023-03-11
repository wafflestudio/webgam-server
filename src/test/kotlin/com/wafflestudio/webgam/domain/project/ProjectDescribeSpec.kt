package com.wafflestudio.webgam.domain.project

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentRequest
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentResponse
import com.wafflestudio.webgam.RestDocsUtils.Companion.requestBody
import com.wafflestudio.webgam.STRING
import com.wafflestudio.webgam.TestUtils
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
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ActiveProfiles("test")
@DisplayName("Project 통합 테스트")
class ProjectDescribeSpec (
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
    }

    init {
        this.describe("프로젝트 조회할 때") {
            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(get("/api/v1/projects/{id}", project.id)
                            .with(authentication(auth))
                    ).andDo(document(
                            "project/get",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            pathParameters(parameterWithName("id").description("프로젝트 ID")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("모든 프로젝트 조회"){
            context("성공하면"){
                it("200 OK") {
                    mockMvc.perform(get("/api/v1/projects")
                            .param("page","0")
                            .param("size","10")
                    ).andDo(document(
                            "project/get-list",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            queryParameters(parameterWithName("page").description("페이지 번호"),
                                    parameterWithName("size").description("페이지 당 프로젝트 수")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("나의 모든 프로젝트 조회"){
            context("성공하면"){
                it("200 OK"){
                    mockMvc.perform(get("/api/v1/projects/me")
                            .with(authentication(auth))
                    ).andDo(document(
                            "project/get-user-list",
                            getDocumentRequest(),
                            getDocumentResponse(),
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("프로젝트 생성할 때") {
            context("성공하면") {
                val request = mapOf("title" to "create-project")

                it("200 OK") {
                    mockMvc.perform(post("/api/v1/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                            "project/create",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            requestBody(
                                    "title" type STRING means "제목",
                            )
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("프로젝트 수정할 때") {
            context("성공하면") {
                val request = mapOf("title" to "patch-project")

                it("200 OK") {
                    mockMvc.perform(patch("/api/v1/projects/{id}", project.id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                            "project/patch",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            pathParameters(parameterWithName("id").description("프로젝트 ID")),
                            requestBody(
                                    "title" type STRING means "제목" isOptional true,
                            )
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("프로젝트 삭제할 때") {
            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(
                            delete("/api/v1/projects/{id}", project.id)
                                    .with(authentication(auth))
                    ).andDo(document(
                            "project/delete",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            pathParameters(parameterWithName("id").description("프로젝트 ID")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }
    }
}