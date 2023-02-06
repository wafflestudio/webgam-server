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
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Tag

@Tag("Unit-Test")
@DisplayName("PageObjectService 단위 테스트")
class PageObjectServiceTest : DescribeSpec() {

    companion object {
        private val projectRepository = mockk<ProjectRepository>()
        private val projectPageRepository = mockk<ProjectPageRepository>()
        private val pageObjectRepository = mockk<PageObjectRepository>()
        private val pageObjectService = PageObjectService(projectRepository, projectPageRepository, pageObjectRepository)
        private val project = mockk<Project>()
        private val nonAccessibleProject = mockk<Project>()
        private val page = ProjectPage(project, "test-page")
        private val nonAccessiblePage = mockk<ProjectPage>()
        private val pageObject = PageObject(page, "test-object", DEFAULT, 0, 0, 0, 0, 0, "", 0, "", null)
        private val nonAccessiblePageObject = PageObject(nonAccessiblePage, "test-object", DEFAULT, 0, 0, 0, 0, 0, "", 0, "", null)
        private val deletedPageObject = mockk<PageObject>()
    }

    override suspend fun beforeSpec(spec: Spec) {
        every { project.isAccessibleTo(any()) } returns true
        every { nonAccessibleProject.isAccessibleTo(any()) } returns false
        every { projectPageRepository.findUndeletedProjectPageById(any()) } returns page
        every { nonAccessiblePage.isAccessibleTo(any()) } returns false
        every { nonAccessiblePage.id } returns 100
        every { deletedPageObject.isDeleted } returns true
    }

    init {
        this.describe("listProjectObjects 호출될 때") {
            context("성공적인 경우") {
                every { projectRepository.findUndeletedProjectById(any()) } returns project
                every { pageObjectRepository.findAllUndeletedPageObjectsInProject(any()) } returns listOf(pageObject, nonAccessiblePageObject)

                val response = withContext(Dispatchers.IO) {
                    pageObjectService.listProjectObjects(1, 1)
                }

                it("해당 ID를 갖는 프로젝트에 있는 PageObject들이 Detailed Response DTO로 반환된다") {
                    response.count shouldBe 1
                    response.data shouldContain DetailedResponse(pageObject)
                }

                it("접근 불가능한 PageObject들은 제외된다") {
                    response.data shouldNotContain DetailedResponse(nonAccessiblePageObject)
                }
            }

            context("해당 ID를 갖는 프로젝트가 존재하지 않거나 삭제됐으면") {
                every { projectRepository.findUndeletedProjectById(any()) } returns null

                it("ProjectNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectNotFoundException> { pageObjectService.listProjectObjects(1, 1) }
                }
            }

            context("해당 ID를 갖는 프로젝트에 접근할 수 없으면") {
                every { projectRepository.findUndeletedProjectById(any()) } returns nonAccessibleProject

                it("NonAccessibleProjectException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectException> { pageObjectService.listProjectObjects(1, 1) }
                }
            }
        }

        this.describe("getObject 호출될 때") {
            context("성공적인 경우") {
                every { pageObjectRepository.findUndeletedPageObjectById(any()) } returns pageObject

                val response = withContext(Dispatchers.IO) {
                    pageObjectService.getObject(1, 1)
                }

                it("PageObject가 Detailed Response DTO로 반환된다") {
                    response shouldBe DetailedResponse(pageObject)
                }
            }

            context("해당 ID를 갖는 PageObject가 존재하지 않거나 삭제됐으면") {
                every { pageObjectRepository.findUndeletedPageObjectById(any()) } returns null

                it("PageObjectNotFoundException 예외를 던진다") {
                    shouldThrow<PageObjectNotFoundException> { pageObjectService.getObject(1, 1) }
                }
            }

            context("해당 ID를 갖는 PageObject에 접근할 수 없으면") {
                every { pageObjectRepository.findUndeletedPageObjectById(any()) } returns nonAccessiblePageObject

                it("NonAccessiblePageObjectException 예외를 던진다") {
                    shouldThrow<NonAccessiblePageObjectException> { pageObjectService.getObject(1, 1) }
                }
            }
        }

        this.describe("createObject 호출될 때") {
            val request = CreateRequest(0, "", DEFAULT, 0, 0, 0, 0, 0, "", 0, "")

            context("성공적인 경우") {
                every { projectPageRepository.findUndeletedProjectPageById(any()) } returns page
                every { pageObjectRepository.save(any()) } returns pageObject

                val response = withContext(Dispatchers.IO) {
                    pageObjectService.createObject(1, request)
                }

                it("생성된 PageObject가 SimpleResponse DTO로 반환된다") {
                    response shouldBe SimpleResponse(pageObject)
                }
            }

            context("해당 ID를 갖는 페이지가 존재하지 않거나 삭제됐으면") {
                every { projectPageRepository.findUndeletedProjectPageById(any()) } returns null

                it("ProjectPageNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectPageNotFoundException> { pageObjectService.createObject(1, request) }
                }
            }

            context("해당 ID를 갖는 페이지에 접근할 수 없으면") {
                every { projectPageRepository.findUndeletedProjectPageById(any()) } returns nonAccessiblePage

                it("NonAccessibleProjectPageException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectPageException> { pageObjectService.createObject(1, request) }
                }
            }
        }

        this.describe("modifyObject 호출될 때") {
            pageObject.width = 20
            pageObject.height = 10
            val request = PatchRequest(null, 30, null, null, null, null, null, null, null)

            context("성공적인 경우") {
                every { pageObjectRepository.findUndeletedPageObjectById(any()) } returns pageObject

                val response = withContext(Dispatchers.IO) {
                    pageObjectService.modifyObject(1, 1, request)
                }

                it("수정된 PageObject가 Detailed Response DTO로 반환된다") {
                    response shouldBe DetailedResponse(pageObject)
                }

                it("request의 값으로 수정된다") {
                    response.width shouldBe 30
                }

                it("request의 NULL은 수정되지 않는다") {
                    response.height shouldBe 10
                }
            }

            context("해당 ID를 갖는 PageObject가 존재하지 않거나 삭제됐으면") {
                every { pageObjectRepository.findUndeletedPageObjectById(any()) } returns null

                it("PageObjectNotFoundException 예외를 던진다") {
                    shouldThrow<PageObjectNotFoundException> { pageObjectService.modifyObject(1, 1, request) }
                }
            }

            context("해당 ID를 갖는 PageObject에 접근할 수 없으면") {
                every { pageObjectRepository.findUndeletedPageObjectById(any()) } returns nonAccessiblePageObject

                it("NonAccessiblePageObjectException 예외를 던진다") {
                    shouldThrow<NonAccessiblePageObjectException> { pageObjectService.modifyObject(1, 1, request) }
                }
            }
        }

        this.describe("deleteObject 호출될 때") {
            context("성공적인 경우") {
                every { pageObjectRepository.findUndeletedPageObjectById(any()) } returns pageObject

                it("반환 값이 없으며, 예외도 던지지 않는다") {
                    shouldNotThrowAny { pageObjectService.deleteObject(1, 1) }
                }

                it("해당 PageObject의 isDeleted는 true가 된다") {
                    pageObject.isDeleted shouldBe true
                }
            }

            context("해당 ID를 갖는 PageObject가 존재하지 않거나 삭제됐으면") {
                every { pageObjectRepository.findUndeletedPageObjectById(any()) } returns null

                it("PageObjectNotFoundException 예외를 던진다") {
                    shouldThrow<PageObjectNotFoundException> { pageObjectService.deleteObject(1, 1) }
                }
            }

            context("해당 ID를 갖는 PageObject에 접근할 수 없으면") {
                every { pageObjectRepository.findUndeletedPageObjectById(any()) } returns nonAccessiblePageObject

                it("NonAccessiblePageObjectException 예외를 던진다") {
                    shouldThrow<NonAccessiblePageObjectException> { pageObjectService.deleteObject(1, 1) }
                }
            }
        }
    }

}