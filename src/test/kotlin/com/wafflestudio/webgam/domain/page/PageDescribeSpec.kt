package com.wafflestudio.webgam.domain.page

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.wafflestudio.webgam.NUMBER
import com.wafflestudio.webgam.RestDocsUtils
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentResponse
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentRequest
import com.wafflestudio.webgam.STRING
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
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
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
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
@DisplayName("Page 통합 테스트")
class PageDescribeSpec (
        @Autowired private val mockMvc: MockMvc,
        @Autowired private val userRepository: UserRepository,
        @Autowired private val projectRepository: ProjectRepository,
        @Autowired private val projectPageRepository: ProjectPageRepository,
): DescribeSpec(){

    override fun extensions() = listOf(SpringExtension)

    final val user = userRepository.save(User("fooId", "foo", "foo@wafflestudio.com", ""))
    private final val dummyUser = userRepository.save(User("barId", "bar", "bar@wafflestudio.com", ""))
    private final val auth = WebgamAuthenticationToken(UserPrincipal(user), "")
    private final val project = projectRepository.save(Project(user, "test-project"))
    private final val dummyProject = projectRepository.save(Project(dummyUser, "test-project"))
    private final val page = projectPageRepository.save(ProjectPage(project, "test-page"))
    private final val dummyPage = projectPageRepository.save(ProjectPage(dummyProject, "test-page"))

    private lateinit var deletedProject: Project
    private lateinit var deletedPage: ProjectPage

    override suspend fun beforeSpec(spec: Spec) {
        val p = Project(user, "deleted-project")
        p.isDeleted = true
        val pp = ProjectPage(project, "deleted-page")
        pp.isDeleted = true

        withContext(Dispatchers.IO) {
            deletedProject = projectRepository.save(p)
            deletedPage = projectPageRepository.save(pp)
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            projectPageRepository.deleteAll()
            projectRepository.deleteAll()
            userRepository.deleteAll()
        }
    }

    companion object {
        private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        //private val validCreateRequest = ProjectPageDto.CreateRequest(1, "page_name") // project.id
        private val validPatchRequest = ProjectPageDto.PatchRequest("page_name_changed")
    }

    init{
        this.describe("프로젝트 페이지 조회할 때"){
            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(get("/api/v1/page/{id}", page.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                            "get-project-page/200",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            pathParameters(parameterWithName("id").description("프로젝트 페이지 ID")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("올바르지 않은 프로젝트 페이지 ID일 때") {
                val errorCode = ErrorType.BadRequest.CONSTRAINT_VIOLATION.code()
                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                            get("/api/v1/page/{id}", "0")
                                    .with(authentication(auth))
                    ).andDo(document(
                            "get-project-page/400-0",
                            getDocumentResponse()
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 프로젝트 페이지에 접근하지 못하는 경우") {
                val errorCode = ErrorType.Forbidden.NON_ACCESSIBLE_PROJECT_PAGE.code()

                it("403 Forbidden, 에러코드 $errorCode") {
                    mockMvc.perform(
                            get("/api/v1/page/{id}", dummyPage.id.toString())
                                    .with(authentication(auth))
                    ).andDo(document(
                            "get-project-page/403-0",
                            getDocumentResponse()
                    )).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 프로젝트가 없거나 삭제 되었을 때") {
                val errorCode = ErrorType.NotFound.PROJECT_PAGE_NOT_FOUND.code()

                it("404 Not Found, 에러코드 $errorCode") {
                    mockMvc.perform(
                            get("/api/v1/page/{id}", deletedPage.id.toString())
                                    .with(authentication(auth))
                    ).andDo(document(
                            "get-project-page/404-0",
                            getDocumentResponse()
                    )).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }
        }

        this.describe("프로젝트 페이지 생성할 때") {
            context("성공하면") {
                val request = mapOf("project_id" to 1, "name" to "name")

                it("200 OK") {
                    mockMvc.perform(post("/api/v1/page")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                            "create-project-page/200",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            RestDocsUtils.requestBody(
                                    "project_id" type NUMBER means "프로젝트 ID",
                                    "name" type STRING means "페이지 이름"
                            )
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("올바르지 않은 값을 넣거나 필수값이 없을 때") {
                val request = mapOf("project_id" to 0L)

                val errorCode = ErrorType.BadRequest.INVALID_FIELD.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                            post("/api/v1/page")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(gson.toJson(request))
                                    .with(authentication(auth))
                    ).andDo(document(
                            "create-project-page/400-0",
                            getDocumentResponse(),
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("올바르지 않은 타입을 넣을 때") {
                val request = mapOf("project_id" to "non int", "name" to "page")

                val errorCode = ErrorType.BadRequest.JSON_PARSE_ERROR.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                            post("/api/v1/page")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(gson.toJson(request))
                                    .with(authentication(auth))
                    ).andDo(document(
                            "create-project-page/400-1",
                            getDocumentResponse(),
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 프로젝트에 접근하지 못하는 경우") {
                val request = mapOf(
                        "project_id" to dummyProject.id,
                        "name" to "create page test"
                )

                val errorCode = ErrorType.Forbidden.NON_ACCESSIBLE_PROJECT.code()

                it("403 Forbidden, 에러코드 $errorCode") {
                    mockMvc.perform(
                            post("/api/v1/page")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(gson.toJson(request))
                                    .with(authentication(auth))
                    ).andDo(document(
                            "create-project-page/403-0",
                            getDocumentResponse()
                    )).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 프로젝트가 없거나 삭제 되었을 때") {
                val request = mapOf(
                        "project_id" to deletedProject.id,
                        "name" to "create page test"
                )

                val errorCode = ErrorType.NotFound.PROJECT_NOT_FOUND.code()

                it("404 Not Found, 에러코드 $errorCode") {
                    mockMvc.perform(
                            post("/api/v1/page")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(gson.toJson(request))
                                    .with(authentication(auth))
                    ).andDo(document(
                            "create-project-page/404-0",
                            getDocumentResponse()
                    )).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }
        }

        this.describe("프로젝트 페이지 수정할 때") {
            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(patch("/api/v1/page/{id}", page.id.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(gson.toJson(validPatchRequest))
                            .with(authentication(auth))
                    ).andDo(document(
                            "patch-project-page/200",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            pathParameters(parameterWithName("id").description("프로젝트 페이지 ID")),
                            RestDocsUtils.requestBody(
                                    "name" type STRING means "페이지 이름"
                            )
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("올바르지 않은 페이지 ID일 때") {
                val errorCode = ErrorType.BadRequest.CONSTRAINT_VIOLATION.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                            patch("/api/v1/page/{id}", "0")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(gson.toJson(validPatchRequest))
                                    .with(authentication(auth))
                    ).andDo(document(
                            "patch-project-page/400-0",
                            getDocumentResponse()
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 페이지에 접근하지 못하는 경우") {

                val errorCode = ErrorType.Forbidden.NON_ACCESSIBLE_PROJECT_PAGE.code()

                it("403 Forbidden, 에러코드 $errorCode") {
                    mockMvc.perform(
                            patch("/api/v1/page/{id}", dummyPage.id.toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(gson.toJson(validPatchRequest))
                                    .with(authentication(auth))
                    ).andDo(document(
                            "patch-project-page/403-0",
                            getDocumentResponse()
                    )).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 페이지가 없거나 삭제 되었을 때") {
                val errorCode = ErrorType.NotFound.PROJECT_PAGE_NOT_FOUND.code()

                it("404 Not Found, 에러코드 $errorCode") {
                    mockMvc.perform(
                            patch("/api/v1/page/{id}", deletedPage.id.toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(gson.toJson(validPatchRequest))
                                    .with(authentication(auth))
                    ).andDo(document(
                            "patch-project-page/404-0",
                            getDocumentResponse()
                    )).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }
        }

        this.describe("프로젝트 페이지 삭제할 때") {
            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(
                            delete("/api/v1/page/{id}", page.id.toString())
                                    .with(authentication(auth))
                    ).andDo(document(
                            "delete-project-page/200",
                            getDocumentRequest(),
                            getDocumentResponse(),
                            pathParameters(parameterWithName("id").description("프로젝트 페이지 ID")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("올바르지 않은 페이지 ID일 때") {
                val errorCode = ErrorType.BadRequest.CONSTRAINT_VIOLATION.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                            delete("/api/v1/page/{id}", "0")
                                    .with(authentication(auth))
                    ).andDo(document(
                            "delete-project-page/400-0",
                            getDocumentResponse()
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 페이지에 접근하지 못하는 경우") {
                val errorCode = ErrorType.Forbidden.NON_ACCESSIBLE_PROJECT_PAGE.code()

                it("403 Forbidden, 에러코드 $errorCode") {
                    mockMvc.perform(
                            delete("/api/v1/page/{id}", dummyPage.id.toString())
                                    .with(authentication(auth))
                    ).andDo(document(
                            "delete-project-page/403-0",
                            getDocumentResponse()
                    )).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 페이지가 없거나 삭제 되었을 때") {
                val errorCode = ErrorType.NotFound.PROJECT_PAGE_NOT_FOUND.code()

                it("404 Not Found, 에러코드 $errorCode") {
                    mockMvc.perform(
                            delete("/api/v1/page/{id}", deletedPage.id.toString())
                                    .with(authentication(auth))
                    ).andDo(document(
                            "delete-project-page/404-0",
                            getDocumentResponse()
                    )).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", Is.`is`(errorCode))
                    ).andDo(print())
                }
            }
        }

    }

}