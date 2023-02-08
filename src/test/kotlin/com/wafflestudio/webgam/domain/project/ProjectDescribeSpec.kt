package com.wafflestudio.webgam.domain.project

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.wafflestudio.webgam.RestDocsUtils.Companion.requestBody
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentResponse
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentRequest
import com.wafflestudio.webgam.STRING
import com.wafflestudio.webgam.domain.project.dto.ProjectDto
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import com.wafflestudio.webgam.type
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hamcrest.core.Is
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
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
@DisplayName("Project 통합 테스트")
class ProjectDescribeSpec (
        @Autowired private val mockMvc: MockMvc,
        @Autowired private val userRepository: UserRepository,
        @Autowired private val projectRepository: ProjectRepository,
): DescribeSpec(){

    override fun extensions() = listOf(SpringExtension)

    final val user = userRepository.save(User("fooId", "foo", "foo@wafflestudio.com", ""))
    private final val dummyUser = userRepository.save(User("barId", "bar", "bar@wafflestudio.com", ""))
    private final val auth = WebgamAuthenticationToken(UserPrincipal(user), "")
    private final val defaultProject = projectRepository.save(Project(user, "default-project"))
    private final val dummyProject = projectRepository.save(Project(dummyUser, "dummy-project"))

    private lateinit var deletedProject: Project


    override suspend fun beforeSpec(spec: Spec) {
        val p = Project(user, "deleted-project")
        p.isDeleted = true

        withContext(Dispatchers.IO) {
            deletedProject = projectRepository.save(p)
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            projectRepository.deleteAll()
            userRepository.deleteAll()
        }
    }

    companion object {
        private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        private val validCreateRequest = ProjectDto.CreateRequest("new_title")
        private val validPatchRequest = ProjectDto.PatchRequest("title_changed")
    }

    init {
        this.describe("프로젝트 조회할 때") {
            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(get("/api/v1/project/{id}", defaultProject.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                            "get-project/200",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            pathParameters(parameterWithName("id").description("프로젝트 ID")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("올바르지 않은 프로젝트 ID일 때") {
                val errorCode = ErrorType.BadRequest.CONSTRAINT_VIOLATION.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                            get("/api/v1/project/{id}", "0")
                                    .with(authentication(auth))
                    ).andDo(document(
                            "get-project/400-0",
                            getDocumentResponse()
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 프로젝트에 접근하지 못하는 경우") {
                val errorCode = ErrorType.Forbidden.NON_ACCESSIBLE_PROJECT.code()

                it("403 Forbidden, 에러코드 $errorCode") {
                    mockMvc.perform(
                            get("/api/v1/project/{id}", dummyProject.id.toString())
                                    .with(authentication(auth))
                    ).andDo(document(
                            "get-project/403-0",
                            getDocumentResponse()
                    )).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 프로젝트가 없거나 삭제 되었을 때") {
                val errorCode = ErrorType.NotFound.PROJECT_NOT_FOUND.code()

                it("404 Not Found, 에러코드 $errorCode") {
                    mockMvc.perform(
                            get("/api/v1/project/{id}", deletedProject.id.toString())
                                    .with(authentication(auth))
                    ).andDo(document(
                            "get-project/404-0",
                            getDocumentResponse()
                    )).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

        }

        this.describe("모든 프로젝트 조회"){
            context("성공하면"){
                it("200 OK") {
                    mockMvc.perform(get("/api/v1/project")
                            .param("page","0")
                            .param("size","10")
                    ).andDo(document(
                            "get-projects/200",
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
                    mockMvc.perform(get("/api/v1/project/me")
                            .with(authentication(auth))
                    ).andDo(document(
                            "get-my-projects/200",
                            getDocumentRequest(),
                            getDocumentResponse(),
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("프로젝트 생성할 때") {
            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(post("/api/v1/project")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(gson.toJson(validCreateRequest))
                            .with(authentication(auth))
                    ).andDo(document(
                            "create-project/200",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            requestBody(
                                    "title" type STRING means "제목",
                            )
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("올바르지 않은 값을 넣거나 필수값이 없을 때") {
                val request = mapOf("title" to "   ")

                val errorCode = ErrorType.BadRequest.INVALID_FIELD.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                            post("/api/v1/project")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(gson.toJson(request))
                                    .with(authentication(auth))
                    ).andDo(document(
                            "create-project/400-0",
                            getDocumentResponse(),
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }
        }

        this.describe("프로젝트 수정할 때") {
            context("성공하면") {

                it("200 OK") {
                    mockMvc.perform(patch("/api/v1/project/{id}", defaultProject.id.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(gson.toJson(validPatchRequest))
                            .with(authentication(auth))
                    ).andDo(document(
                            "patch-project/200",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            pathParameters(parameterWithName("id").description("프로젝트 ID")),
                            requestBody(
                                    "title" type STRING means "제목",
                            )
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("올바르지 않은 값을 넣을 때") {
                val request = mapOf("title" to "   ")

                val errorCode = ErrorType.BadRequest.INVALID_FIELD.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                            patch("/api/v1/project/{id}", defaultProject.id.toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(gson.toJson(request))
                                    .with(authentication(auth))
                    ).andDo(document(
                            "patch-project/400-0",
                            getDocumentResponse(),
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("올바르지 않은 프로젝트 ID일 때") {
                val errorCode = ErrorType.BadRequest.CONSTRAINT_VIOLATION.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                            patch("/api/v1/project/{id}", "0")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(gson.toJson(validPatchRequest))
                                    .with(authentication(auth))
                    ).andDo(document(
                            "patch-project/400-1",
                            getDocumentResponse()
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 프로젝트에 접근하지 못하는 경우") {
                val errorCode = ErrorType.Forbidden.NON_ACCESSIBLE_PROJECT.code()

                it("403 Forbidden, 에러코드 $errorCode") {
                    mockMvc.perform(
                            patch("/api/v1/project/{id}", dummyProject.id.toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(gson.toJson(validPatchRequest))
                                    .with(authentication(auth))
                    ).andDo(document(
                            "patch-project/403-0",
                            getDocumentResponse()
                    )).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 프로젝트가 없거나 삭제 되었을 때") {
                val errorCode = ErrorType.NotFound.PROJECT_NOT_FOUND.code()

                it("404 Not Found, 에러코드 $errorCode") {
                    mockMvc.perform(
                            patch("/api/v1/project/{id}", deletedProject.id.toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(gson.toJson(validPatchRequest))
                                    .with(authentication(auth))
                    ).andDo(document(
                            "patch-project/404-0",
                            getDocumentResponse()
                    )).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }
        }

        this.describe("프로젝트 삭제할 때") {
            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(
                            delete("/api/v1/project/{id}", defaultProject.id.toString())
                                    .with(authentication(auth))
                    ).andDo(document(
                            "delete-project/200",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            pathParameters(parameterWithName("id").description("프로젝트 ID")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("올바르지 않은 프로젝트 ID일 때") {
                val errorCode = ErrorType.BadRequest.CONSTRAINT_VIOLATION.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                            delete("/api/v1/project/{id}", "0")
                                    .with(authentication(auth))
                    ).andDo(document(
                            "delete-project/400-0",
                            getDocumentResponse()
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 프로젝트에 접근하지 못하는 경우") {
                val errorCode = ErrorType.Forbidden.NON_ACCESSIBLE_PROJECT.code()

                it("403 Forbidden, 에러코드 $errorCode") {
                    mockMvc.perform(
                            delete("/api/v1/project/{id}", dummyProject.id.toString())
                                    .with(authentication(auth))
                    ).andDo(document(
                            "delete-project/403-0",
                            getDocumentResponse()
                    )).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 프로젝트가 없거나 삭제 되었을 때") {
                val errorCode = ErrorType.NotFound.PROJECT_NOT_FOUND.code()

                it("404 Not Found, 에러코드 $errorCode") {
                    mockMvc.perform(
                            delete("/api/v1/project/{id}", deletedProject.id.toString())
                                    .with(authentication(auth))
                    ).andDo(document(
                            "delete-project/404-0",
                            getDocumentResponse()
                    )).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

        }

    }

}