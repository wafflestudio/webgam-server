package com.wafflestudio.webgam.domain.`object`.service

import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto.*
import com.wafflestudio.webgam.domain.`object`.exception.NonAccessiblePageObjectException
import com.wafflestudio.webgam.domain.`object`.exception.PageObjectNotFoundException
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType.DEFAULT
import com.wafflestudio.webgam.domain.`object`.repository.PageObjectRepository
import com.wafflestudio.webgam.domain.page.exception.NonAccessibleProjectPageException
import com.wafflestudio.webgam.domain.page.exception.ProjectPageNotFoundException
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import com.wafflestudio.webgam.domain.project.exception.NonAccessibleProjectException
import com.wafflestudio.webgam.domain.project.exception.ProjectNotFoundException
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

@DisplayName("PageObjectService 단위 테스트")
class PageObjectServiceTest : DescribeSpec() {

    companion object {
        private val projectRepository = mockk<ProjectRepository>()
        private val projectPageRepository = mockk<ProjectPageRepository>()
        private val pageObjectRepository = mockk<PageObjectRepository>()
        private val pageObjectService = PageObjectService(projectRepository, projectPageRepository, pageObjectRepository)
        private val project = mockk<Project>()
        private val nonAccessibleProject = mockk<Project>()
        private val page = mockk<ProjectPage>()
        private val nonAccessiblePage = mockk<ProjectPage>()
        private val pageObject = PageObject(page, "object", DEFAULT, 0, 0, 0, 0, 0, 0, null, null, null, null, null, null, null, null, null, null)
        private val nonAccessiblePageObject = mockk<PageObject>()

        private const val USER_ID = 1L
        private const val NORMAL = 1L
        private const val DELETED = 2L
        private const val NON_ACCESSIBLE = 3L
    }

    override suspend fun beforeSpec(spec: Spec) {
        every { projectRepository.findUndeletedProjectById(NORMAL) } returns project
        every { projectRepository.findUndeletedProjectById(DELETED) } returns null
        every { projectRepository.findUndeletedProjectById(NON_ACCESSIBLE) } returns nonAccessibleProject

        every { projectPageRepository.findUndeletedProjectPageById(NORMAL) } returns page
        every { projectPageRepository.findUndeletedProjectPageById(DELETED) } returns null
        every { projectPageRepository.findUndeletedProjectPageById(NON_ACCESSIBLE) } returns nonAccessiblePage

        every { pageObjectRepository.save(any()) } returns pageObject
        every { pageObjectRepository.findUndeletedPageObjectById(NORMAL) } returns pageObject
        every { pageObjectRepository.findUndeletedPageObjectById(DELETED) } returns null
        every { pageObjectRepository.findUndeletedPageObjectById(NON_ACCESSIBLE) } returns nonAccessiblePageObject
        every { pageObjectRepository.findAllUndeletedPageObjectsInProject(NORMAL) } returns listOf(pageObject)

        every { project.isAccessibleTo(USER_ID) } returns true
        every { page.isAccessibleTo(USER_ID) } returns true

        every { nonAccessibleProject.isAccessibleTo(USER_ID) } returns false
        every { nonAccessiblePage.isAccessibleTo(USER_ID) } returns false
        every { nonAccessiblePageObject.isAccessibleTo(USER_ID) } returns false

        every { page.id } returns NORMAL
        every { page.objects } returns mutableListOf()
    }

    init {
        this.describe("listProjectObjects 호출될 때") {
            context("성공적인 경우") {
                it("해당 ID를 갖는 프로젝트에 있는 PageObject들이 Detailed Response DTO로 반환된다") {
                    val response = shouldNotThrowAny { pageObjectService.listProjectObjects(USER_ID, NORMAL) }
                    response.data shouldContain DetailedResponse(pageObject)
                }
            }

            context("해당 ID를 갖는 프로젝트가 존재하지 않거나 삭제됐으면") {
                it("ProjectNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectNotFoundException> { pageObjectService.listProjectObjects(USER_ID, DELETED) }
                }
            }

            context("해당 ID를 갖는 프로젝트에 접근할 수 없으면") {
                it("NonAccessibleProjectException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectException> { pageObjectService.listProjectObjects(USER_ID, NON_ACCESSIBLE) }
                }
            }
        }

        this.describe("getObject 호출될 때") {
            context("성공적인 경우") {
                it("PageObject가 Detailed Response DTO로 반환된다") {
                    val response = shouldNotThrowAny { pageObjectService.getObject(USER_ID, NORMAL) }
                    response shouldBe DetailedResponse(pageObject)
                }
            }

            context("해당 ID를 갖는 PageObject가 존재하지 않거나 삭제됐으면") {
                it("PageObjectNotFoundException 예외를 던진다") {
                    shouldThrow<PageObjectNotFoundException> { pageObjectService.getObject(USER_ID, DELETED) }
                }
            }

            context("해당 ID를 갖는 PageObject에 접근할 수 없으면") {
                it("NonAccessiblePageObjectException 예외를 던진다") {
                    shouldThrow<NonAccessiblePageObjectException> { pageObjectService.getObject(USER_ID, NON_ACCESSIBLE) }
                }
            }
        }

        this.describe("createObject 호출될 때") {
            context("성공적인 경우") {
                val request = CreateRequest(NORMAL, "create-object", DEFAULT, 0, 0, 0, 0, 0, 0, null, null, null, null, null, null, null, null, null, null)

                it("생성된 PageObject가 SimpleResponse DTO로 반환된다") {
                    val response = shouldNotThrowAny { pageObjectService.createObject(USER_ID, request) }
                    response shouldBe SimpleResponse(pageObject)
                }
            }

            context("해당 ID를 갖는 페이지가 존재하지 않거나 삭제됐으면") {
                val request = CreateRequest(DELETED, "create-object", DEFAULT, 0, 0, 0, 0, 0, 0, null, null, null, null, null, null, null, null, null, null)

                it("ProjectPageNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectPageNotFoundException> { pageObjectService.createObject(USER_ID, request) }
                }
            }

            context("해당 ID를 갖는 페이지에 접근할 수 없으면") {
                val request = CreateRequest(NON_ACCESSIBLE, "create-object", DEFAULT, 0, 0, 0, 0, 0, 0, null, null, null, null, null, null, null, null, null, null)

                it("NonAccessibleProjectPageException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectPageException> { pageObjectService.createObject(USER_ID, request) }
                }
            }
        }

        this.describe("modifyObject 호출될 때") {
            val request = PatchRequest(null, 30, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)

            context("성공적인 경우") {
                it("수정된 PageObject가 Detailed Response DTO로 반환된다") {
                    val response = shouldNotThrowAny { pageObjectService.modifyObject(USER_ID, NORMAL, request) }
                    response shouldBe DetailedResponse(pageObject)
                }
            }

            context("해당 ID를 갖는 PageObject가 존재하지 않거나 삭제됐으면") {
                it("PageObjectNotFoundException 예외를 던진다") {
                    shouldThrow<PageObjectNotFoundException> { pageObjectService.modifyObject(USER_ID, DELETED, request) }
                }
            }

            context("해당 ID를 갖는 PageObject에 접근할 수 없으면") {
                it("NonAccessiblePageObjectException 예외를 던진다") {
                    shouldThrow<NonAccessiblePageObjectException> { pageObjectService.modifyObject(USER_ID, NON_ACCESSIBLE, request) }
                }
            }
        }

        this.describe("deleteObject 호출될 때") {
            context("성공적인 경우") {
                it("반환 값이 없으며, 예외도 던지지 않는다") {
                    shouldNotThrowAny { pageObjectService.deleteObject(USER_ID, NORMAL) }
                }
            }

            context("해당 ID를 갖는 PageObject가 존재하지 않거나 삭제됐으면") {
                it("PageObjectNotFoundException 예외를 던진다") {
                    shouldThrow<PageObjectNotFoundException> { pageObjectService.deleteObject(USER_ID, DELETED) }
                }
            }

            context("해당 ID를 갖는 PageObject에 접근할 수 없으면") {
                it("NonAccessiblePageObjectException 예외를 던진다") {
                    shouldThrow<NonAccessiblePageObjectException> { pageObjectService.deleteObject(USER_ID, NON_ACCESSIBLE) }
                }
            }
        }
    }
}