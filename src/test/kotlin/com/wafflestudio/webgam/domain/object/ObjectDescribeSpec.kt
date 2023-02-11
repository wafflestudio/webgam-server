package com.wafflestudio.webgam.domain.`object`

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.wafflestudio.webgam.ENUM
import com.wafflestudio.webgam.NUMBER
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentRequest
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentResponse
import com.wafflestudio.webgam.RestDocsUtils.Companion.requestBody
import com.wafflestudio.webgam.STRING
import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import com.wafflestudio.webgam.domain.event.model.TransitionType
import com.wafflestudio.webgam.domain.event.repository.ObjectEventRepository
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType.*
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType.DEFAULT
import com.wafflestudio.webgam.domain.`object`.repository.PageObjectRepository
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.*
import com.wafflestudio.webgam.global.common.exception.ErrorType.Forbidden.*
import com.wafflestudio.webgam.global.common.exception.ErrorType.NotFound.*
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import com.wafflestudio.webgam.type
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*
import org.springframework.restdocs.request.RequestDocumentation.*
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
@DisplayName("Object 통합 테스트")
class ObjectDescribeSpec(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val projectRepository: ProjectRepository,
    @Autowired private val projectPageRepository: ProjectPageRepository,
    @Autowired private val pageObjectRepository: PageObjectRepository,
    @Autowired private val objectEventRepository: ObjectEventRepository,
): DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    final val user = userRepository.save(User("fooId", "foo", "foo@wafflestudio.com", ""))
    private final val dummyUser = userRepository.save(User("barId", "bar", "bar@wafflestudio.com", ""))
    private final val auth = WebgamAuthenticationToken(UserPrincipal(user), "")
    private final val project = projectRepository.save(Project(user, "test-project"))
    private final val dummyProject = projectRepository.save(Project(dummyUser, "test-project"))
    private final val page = projectPageRepository.save(ProjectPage(project, "test-page"))
    private final val dummyPage = projectPageRepository.save(ProjectPage(dummyProject, "test-page"))
    private final val defaultObject = pageObjectRepository.save(PageObject(page, "default-object", DEFAULT,
        10, 10, 0, 0, 0, null, null, null, null))
    private final val dummyObject = pageObjectRepository.save(PageObject(dummyPage, "default-object", DEFAULT,
        10, 10, 0, 0, 0, null, null, null, null))

    private lateinit var deletedProject: Project
    private lateinit var deletedPage: ProjectPage
    private lateinit var deletedObject: PageObject

    override suspend fun beforeSpec(spec: Spec) {
        val p = Project(user, "deleted-project")
        p.isDeleted = true
        val pp = ProjectPage(project, "deleted-page")
        pp.isDeleted = true
        val po = PageObject(page, "deleted-object", DEFAULT, 10, 10, 0, 0, 0, null, null, null, null)
        po.isDeleted = true

        withContext(Dispatchers.IO) {
            deletedProject = projectRepository.save(p)
            deletedPage = projectPageRepository.save(pp)
            deletedObject = pageObjectRepository.save(po)
            objectEventRepository.save(ObjectEvent(defaultObject, null, TransitionType.DEFAULT))
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            pageObjectRepository.deleteAll()
            projectPageRepository.deleteAll()
            projectRepository.deleteAll()
            userRepository.deleteAll()
        }
    }

    companion object {
        private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    }

    init {
        this.describe("프로젝트 내 오브젝트 조회할 때") {
            context("성공하면") {
                withContext(Dispatchers.IO) {
                    val objects = listOf(
                        PageObject(page, "image-object", IMAGE, 100, 20, 30, 40, 1, null, null, "http://image-source.url", null),
                        PageObject(page, "text-object", TEXT, 30, 60, -10, -20, 2, "some text", 16, null, null)
                    )
                    pageObjectRepository.saveAll(objects)
                }

                it("200 OK") {
                    mockMvc.perform(
                        get("/api/v1/objects")
                            .param("project-id", project.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "get-project-objects/200",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(parameterWithName("project-id").description("조회할 프로젝트 ID")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("올바르지 않은 프로젝트 ID일 때") {
                val errorCode = CONSTRAINT_VIOLATION.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                        get("/api/v1/objects")
                            .param("project-id", "0")
                            .with(authentication(auth))
                    ).andDo(document(
                        "get-project-objects/400-0",
                        getDocumentResponse()
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 프로젝트에 접근하지 못하는 경우") {
                val errorCode = NON_ACCESSIBLE_PROJECT.code()

                it("403 Forbidden, 에러코드 $errorCode") {
                    mockMvc.perform(
                        get("/api/v1/objects")
                            .param("project-id", dummyProject.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "get-project-objects/403-0",
                        getDocumentResponse()
                    )).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 프로젝트가 없거나 삭제 되었을 때") {
                val errorCode = PROJECT_NOT_FOUND.code()

                it("404 Not Found, 에러코드 $errorCode") {
                    mockMvc.perform(
                        get("/api/v1/objects")
                            .param("project-id", deletedProject.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "get-project-objects/404-0",
                        getDocumentResponse()
                    )).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }
        }

        this.describe("오브젝트 생성할 때") {
            context("성공하면") {
                val request = mapOf(
                    "page_id" to page.id,
                    "name" to "create object test: image",
                    "type" to IMAGE,
                    "width" to 200,
                    "height" to 50,
                    "x_position" to 20,
                    "y_position" to -10,
                    "z_index" to 2,
                    "text_content" to null,
                    "font_size" to null,
                    "image_source" to "http://sampe-image.url"
                )

                it("200 OK") {
                    mockMvc.perform(
                        post("/api/v1/objects")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "create-object/200",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestBody(
                            "page_id" type NUMBER means "페이지 ID" formattedAs "0 초과",
                            "name" type STRING means "오브젝트 이름",
                            "type" type ENUM(PageObjectType::class) means "오브젝트 타입" withDefaultValue "DEFAULT",
                            "width" type NUMBER means "너비" formattedAs "0 초과",
                            "height" type NUMBER means "높이" formattedAs "0 초과",
                            "x_position" type NUMBER means "x축 위치",
                            "y_position" type NUMBER means "y축 위치",
                            "z_index" type NUMBER means "깊이" formattedAs "0 이상",
                            "text_content" type STRING means "텍스트 내용" isOptional true,
                            "font_size" type NUMBER means "텍스트 폰트 크기" isOptional true formattedAs "0 초과",
                            "image_source" type STRING means "이미지 URL" isOptional true formattedAs "유효한 URL",
                        )
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("올바르지 않은 값을 넣거나 필수값이 없을 때") {
                val request = mapOf("page_id" to "0")

                val errorCode = INVALID_FIELD.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                        post("/api/v1/objects")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "create-object/400-0",
                        getDocumentResponse(),
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }

            context("올바르지 않은 타입을 넣을 때") {
                val request = mapOf("page_id" to "non int")

                val errorCode = JSON_PARSE_ERROR.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                        post("/api/v1/objects")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "create-object/400-1",
                        getDocumentResponse(),
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 페이지에 접근하지 못하는 경우") {
                val request = mapOf(
                    "page_id" to dummyPage.id,
                    "name" to "create object test: image",
                    "type" to IMAGE,
                    "width" to 200,
                    "height" to 50,
                    "x_position" to 20,
                    "y_position" to -10,
                    "z_index" to 2,
                    "text_content" to null,
                    "font_size" to null,
                    "image_source" to "http://sampe-image.url"
                )

                val errorCode = NON_ACCESSIBLE_PROJECT_PAGE.code()

                it("403 Forbidden, 에러코드 $errorCode") {
                    mockMvc.perform(
                        post("/api/v1/objects")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "create-object/403-0",
                        getDocumentResponse()
                    )).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 페이지가 없거나 삭제 되었을 때") {
                val request = mapOf(
                    "page_id" to deletedPage.id,
                    "name" to "create object test: image",
                    "type" to IMAGE,
                    "width" to 200,
                    "height" to 50,
                    "x_position" to 20,
                    "y_position" to -10,
                    "z_index" to 2,
                    "text_content" to null,
                    "font_size" to null,
                    "image_source" to "http://sampe-image.url"
                )

                val errorCode = PROJECT_PAGE_NOT_FOUND.code()

                it("404 Not Found, 에러코드 $errorCode") {
                    mockMvc.perform(
                        post("/api/v1/objects")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "create-object/404-0",
                        getDocumentResponse()
                    )).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }
        }

        this.describe("오브젝트 조회할 때") {
            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(
                        get("/api/v1/objects/{id}", defaultObject.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "get-object/200",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        pathParameters(parameterWithName("id").description("오브젝트 ID"))
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("올바르지 않은 오브젝트 ID일 때") {
                val errorCode = CONSTRAINT_VIOLATION.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                        get("/api/v1/objects/{id}", "0")
                            .with(authentication(auth))
                    ).andDo(document(
                        "get-object/400-0",
                        getDocumentResponse()
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 오브젝트에 접근하지 못하는 경우") {
                val errorCode = NON_ACCESSIBLE_PAGE_OBJECT.code()

                it("403 Forbidden, 에러코드 $errorCode") {
                    mockMvc.perform(
                        get("/api/v1/objects/{id}", dummyObject.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "get-object/403-0",
                        getDocumentResponse()
                    )).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 오브젝트가 없거나 삭제 되었을 때") {
                val errorCode = PAGE_OBJECT_NOT_FOUND.code()

                it("404 Not Found, 에러코드 $errorCode") {
                    mockMvc.perform(
                        get("/api/v1/objects/{id}", deletedObject.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "get-object/404-0",
                        getDocumentResponse()
                    )).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }
        }

        this.describe("오브젝트 수정할 때") {
            context("성공하면") {
                val request = mapOf(
                    "type" to TEXT,
                    "width" to 20,
                    "height" to 100,
                    "x_position" to -20,
                    "y_position" to 10,
                    "z_index" to 0,
                    "text_content" to "new text",
                    "font_size" to 20,
                    "image_source" to ""
                )

                it("200 OK") {
                    mockMvc.perform(
                        patch("/api/v1/objects/{id}", defaultObject.id.toString())
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "patch-object/200",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        pathParameters(parameterWithName("id").description("오브젝트 ID")),
                        requestBody(
                            "type" type ENUM(PageObjectType::class) means "오브젝트 타입" isOptional false,
                            "width" type NUMBER means "너비" formattedAs "0 초과" isOptional false,
                            "height" type NUMBER means "높이" formattedAs "0 초과" isOptional false,
                            "x_position" type NUMBER means "x축 위치" isOptional false,
                            "y_position" type NUMBER means "y축 위치" isOptional false,
                            "z_index" type NUMBER means "깊이" formattedAs "0 이상" isOptional false,
                            "text_content" type STRING means "텍스트 내용" isOptional true,
                            "font_size" type NUMBER means "텍스트 폰트 크기" isOptional true formattedAs "0 초과",
                            "image_source" type STRING means "이미지 URL" isOptional true formattedAs "유효한 URL",
                        )
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("올바르지 않은 값을 넣을 때") {
                val request = mapOf("width" to "0")

                val errorCode = INVALID_FIELD.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                        patch("/api/v1/objects/{id}", defaultObject.id.toString())
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "patch-object/400-0",
                        getDocumentResponse(),
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }

            context("올바르지 않은 타입을 넣을 때") {
                val request = mapOf("width" to "non int")

                val errorCode = JSON_PARSE_ERROR.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                        patch("/api/v1/objects/{id}", defaultObject.id.toString())
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "patch-object/400-1",
                        getDocumentResponse(),
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }

            context("올바르지 않은 오브젝트 ID일 때") {
                val request = mapOf("width" to "1")

                val errorCode = CONSTRAINT_VIOLATION.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                        patch("/api/v1/objects/{id}", "0")
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "patch-object/400-0",
                        getDocumentResponse()
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 오브젝트에 접근하지 못하는 경우") {
                val request = mapOf("width" to "1")

                val errorCode = NON_ACCESSIBLE_PAGE_OBJECT.code()

                it("403 Forbidden, 에러코드 $errorCode") {
                    mockMvc.perform(
                        patch("/api/v1/objects/{id}", dummyObject.id.toString())
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "patch-object/403-0",
                        getDocumentResponse()
                    )).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 오브젝트가 없거나 삭제 되었을 때") {
                val request = mapOf("width" to "1")

                val errorCode = PAGE_OBJECT_NOT_FOUND.code()

                it("404 Not Found, 에러코드 $errorCode") {
                    mockMvc.perform(
                        patch("/api/v1/objects/{id}", deletedObject.id.toString())
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "patch-object/404-0",
                        getDocumentResponse()
                    )).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }
        }

        this.describe("오브젝트 삭제할 때") {
            context("성공하면") {
                it("200 OK") {
                    mockMvc.perform(
                        delete("/api/v1/objects/{id}", defaultObject.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "delete-object/200",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        pathParameters(parameterWithName("id").description("오브젝트 ID")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }

            context("올바르지 않은 오브젝트 ID일 때") {
                val errorCode = CONSTRAINT_VIOLATION.code()

                it("400 Bad Request, 에러코드 $errorCode") {
                    mockMvc.perform(
                        delete("/api/v1/objects/{id}", "0")
                            .with(authentication(auth))
                    ).andDo(document(
                        "delete-object/400-0",
                        getDocumentResponse()
                    )).andExpect(status().isBadRequest
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 오브젝트에 접근하지 못하는 경우") {
                val errorCode = NON_ACCESSIBLE_PAGE_OBJECT.code()

                it("403 Forbidden, 에러코드 $errorCode") {
                    mockMvc.perform(
                        delete("/api/v1/objects/{id}", dummyObject.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "delete-object/403-0",
                        getDocumentResponse()
                    )).andExpect(status().isForbidden
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }

            context("해당 ID를 갖는 오브젝트가 없거나 삭제 되었을 때") {
                val errorCode = PAGE_OBJECT_NOT_FOUND.code()

                it("404 Not Found, 에러코드 $errorCode") {
                    mockMvc.perform(
                        delete("/api/v1/objects/{id}", deletedObject.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "delete-object/404-0",
                        getDocumentResponse()
                    )).andExpect(status().isNotFound
                    ).andExpect(jsonPath("$.error_code", `is`(errorCode))
                    ).andDo(print())
                }
            }
        }
    }
}