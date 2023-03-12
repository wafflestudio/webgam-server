package com.wafflestudio.webgam.domain.page.service

import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto.*
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
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

@DisplayName("ProjectPageService 단위 테스트")
class ProjectPageServiceTest: DescribeSpec() {
    companion object {
        private val projectPageRepository = mockk<ProjectPageRepository>()
        private val projectRepository = mockk<ProjectRepository>()
        private val projectPageService = ProjectPageService(projectPageRepository, projectRepository)
        private val project = mockk<Project>()
        private val nonAccessibleProject = mockk<Project>()
        private val page = ProjectPage(project, "test-page")
        private val nonAccessiblePage = mockk<ProjectPage>()

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

        every { projectPageRepository.save(any()) } returns page

        every { nonAccessibleProject.isAccessibleTo(any()) } returns false
        every { nonAccessiblePage.isAccessibleTo(any()) } returns false

        every { project.isAccessibleTo(USER_ID) } returns true
        every { project.id } returns NORMAL
        every { project.pages } returns mutableListOf()
    }

    init {
        this.describe("getProjectPage 호출될 때") {
            context("정상적인 경우") {
                it("페이지가 DetailedResponse로 반환된다") {
                    val response = shouldNotThrowAny { projectPageService.getProjectPage(USER_ID, NORMAL) }
                    response shouldBe DetailedResponse(page)
                }
            }

            context("페이지가 삭제되거나 없는 경우") {
                it("ProjectPageNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectPageNotFoundException> { projectPageService.getProjectPage(USER_ID, DELETED) }
                }
            }

            context("페이지에 접근 못하는 경우") {
                it("NonAccessibleProjectPageException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectPageException> { projectPageService.getProjectPage(USER_ID, NON_ACCESSIBLE) }
                }
            }
        }

        this.describe("createProjectPage 호출될 때") {
            context("정상적인 경우") {
                val request = CreateRequest(NORMAL, "test-page")

                it("페이지가 DetailedResponse로 반환된다") {
                    val response = shouldNotThrowAny { projectPageService.createProjectPage(USER_ID, request) }
                    response shouldBe DetailedResponse(page)
                }
            }

            context("프로젝트가 삭제되거나 없는 경우") {
                val request = CreateRequest(DELETED, "test-page")

                it("ProjectNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectNotFoundException> { projectPageService.createProjectPage(USER_ID, request) }
                }
            }

            context("프로젝트에 접근 못하는 경우") {
                val request = CreateRequest(NON_ACCESSIBLE, "test-page")

                it("NonAccessibleProjectException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectException> { projectPageService.createProjectPage(USER_ID, request) }
                }
            }
        }

        this.describe("patchProjectPage 호출될 때") {
            val request = PatchRequest("test-page-patch")

            context("정상적인 경우") {
                it("페이지가 DetailedResponse로 반환된다") {
                    val response = shouldNotThrowAny { projectPageService.patchProjectPage(USER_ID, NORMAL, request) }
                    response shouldBe DetailedResponse(page)
                }
            }

            context("페이지가 삭제되거나 없는 경우") {
                it("ProjectPageNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectPageNotFoundException> { projectPageService.patchProjectPage(USER_ID, DELETED, request) }
                }
            }

            context("페이지에 접근 못하는 경우") {
                it("NonAccessibleProjectPageException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectPageException> { projectPageService.patchProjectPage(USER_ID, NON_ACCESSIBLE, request) }
                }
            }
        }

        this.describe("deleteProjectPage 호출될 때") {
            context("정상적인 경우") {
                it("아무런 예외도 발생하지 않으며 리턴 값도 없다") {
                    val ret = shouldNotThrowAny { projectPageService.deleteProjectPage(USER_ID, NORMAL) }
                    ret shouldBe Unit
                }
            }

            context("페이지가 삭제되거나 없는 경우") {
                it("ProjectPageNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectPageNotFoundException> { projectPageService.deleteProjectPage(USER_ID, DELETED) }
                }
            }

            context("페이지에 접근 못하는 경우") {
                it("NonAccessibleProjectPageException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectPageException> { projectPageService.deleteProjectPage(USER_ID, NON_ACCESSIBLE) }
                }
            }
        }
    }

}