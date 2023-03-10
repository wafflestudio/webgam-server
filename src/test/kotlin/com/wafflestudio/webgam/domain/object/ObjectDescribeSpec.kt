package com.wafflestudio.webgam.domain.`object`

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.wafflestudio.webgam.*
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentRequest
import com.wafflestudio.webgam.RestDocsUtils.Companion.getDocumentResponse
import com.wafflestudio.webgam.RestDocsUtils.Companion.requestBody
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType.*
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.*
import com.wafflestudio.webgam.global.common.exception.ErrorType.Forbidden.*
import com.wafflestudio.webgam.global.common.exception.ErrorType.NotFound.*
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
import org.springframework.http.MediaType.APPLICATION_JSON
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
@DisplayName("Object ?????? ?????????")
class ObjectDescribeSpec(
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
        private val pageObject = page.objects.first()
    }

    init {
        this.describe("???????????? ??? ???????????? ????????? ???") {
            context("????????????") {
                it("200 OK") {
                    mockMvc.perform(
                        get("/api/v1/objects")
                            .param("project-id", project.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "object/get-list",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(parameterWithName("project-id").description("????????? ???????????? ID")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("???????????? ????????? ???") {
            context("????????????") {
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
                        "object/create",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestBody(
                            "page_id" type NUMBER means "????????? ID" formattedAs "0 ??????",
                            "name" type STRING means "???????????? ??????",
                            "type" type ENUM(PageObjectType::class) means "???????????? ??????" withDefaultValue "DEFAULT",
                            "width" type NUMBER means "??????" formattedAs "0 ??????",
                            "height" type NUMBER means "??????" formattedAs "0 ??????",
                            "x_position" type NUMBER means "x??? ??????",
                            "y_position" type NUMBER means "y??? ??????",
                            "z_index" type NUMBER means "??????" formattedAs "0 ??????",
                            "text_content" type STRING means "????????? ??????" isOptional true,
                            "font_size" type NUMBER means "????????? ?????? ??????" isOptional true formattedAs "0 ??????",
                            "image_source" type STRING means "????????? URL" isOptional true formattedAs "????????? URL",
                        )
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("???????????? ????????? ???") {
            context("????????????") {
                it("200 OK") {
                    mockMvc.perform(
                        get("/api/v1/objects/{id}", pageObject.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "object/get",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        pathParameters(parameterWithName("id").description("???????????? ID"))
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("???????????? ????????? ???") {
            context("????????????") {
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
                        patch("/api/v1/objects/{id}", pageObject.id.toString())
                            .contentType(APPLICATION_JSON)
                            .content(gson.toJson(request))
                            .with(authentication(auth))
                    ).andDo(document(
                        "object/patch",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        pathParameters(parameterWithName("id").description("???????????? ID")),
                        requestBody(
                            "type" type ENUM(PageObjectType::class) means "???????????? ??????" isOptional false,
                            "width" type NUMBER means "??????" formattedAs "0 ??????" isOptional false,
                            "height" type NUMBER means "??????" formattedAs "0 ??????" isOptional false,
                            "x_position" type NUMBER means "x??? ??????" isOptional false,
                            "y_position" type NUMBER means "y??? ??????" isOptional false,
                            "z_index" type NUMBER means "??????" formattedAs "0 ??????" isOptional false,
                            "text_content" type STRING means "????????? ??????" isOptional true,
                            "font_size" type NUMBER means "????????? ?????? ??????" isOptional true formattedAs "0 ??????",
                            "image_source" type STRING means "????????? URL" isOptional true formattedAs "????????? URL",
                        )
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }

        this.describe("???????????? ????????? ???") {
            context("????????????") {
                it("200 OK") {
                    mockMvc.perform(
                        delete("/api/v1/objects/{id}", pageObject.id.toString())
                            .with(authentication(auth))
                    ).andDo(document(
                        "object/delete",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        pathParameters(parameterWithName("id").description("???????????? ID")),
                    )).andExpect(status().isOk).andDo(print())
                }
            }
        }
    }
}