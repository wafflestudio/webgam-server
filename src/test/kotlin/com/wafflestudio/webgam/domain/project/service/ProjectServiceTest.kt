package com.wafflestudio.webgam.domain.project.service

import com.wafflestudio.webgam.domain.project.dto.ProjectDto.*
import com.wafflestudio.webgam.domain.project.exception.NonAccessibleProjectException
import com.wafflestudio.webgam.domain.project.exception.ProjectNotFoundException
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl

@DisplayName("ProjectService 단위 테스트")
class ProjectServiceTest: DescribeSpec() {

    companion object {
        private val projectRepository = mockk<ProjectRepository>()
        private val userRepository = mockk<UserRepository>()
        private val projectService = ProjectService(projectRepository, userRepository)
        private val user = mockk<User>()
        private val project = Project(user, "project")
        private val nonAccessibleProject = mockk<Project>()

        private const val USER_ID = 1L
        private const val NORMAL = 1L
        private const val DELETED = 2L
        private const val NON_ACCESSIBLE = 3L
    }

    override suspend fun beforeSpec(spec: Spec) {
        every { projectRepository.save(any()) } returns project
        every { projectRepository.findUndeletedProjectById(NORMAL) } returns project
        every { projectRepository.findUndeletedProjectById(DELETED) } returns null
        every { projectRepository.findUndeletedProjectById(NON_ACCESSIBLE) } returns nonAccessibleProject
        every { projectRepository.findUndeletedAll(any<PageRequest>()) } returns SliceImpl(listOf(project))
        every { projectRepository.findUndeletedAllByOwnerIdEquals(USER_ID) } returns listOf(project)

        every { userRepository.findUserById(USER_ID) } returns user

        every { user.id } returns USER_ID
        every { user.userId } returns "user-id"
        every { user.username } returns "username"
        every { user.email } returns "user@email.com"
        every { user.projects } returns mutableListOf()

        every { nonAccessibleProject.isAccessibleTo(USER_ID) } returns false
    }

    init {
        this.describe("getProject 호출될 때") {
            context("정상적인 경우") {
                it("프로젝트가 DetailedResponse로 반환된다") {
                    val response = shouldNotThrowAny { projectService.getProject(USER_ID, NORMAL) }
                    response shouldBe DetailedResponse(project)
                }
            }

            context("프로젝트가 삭제되거나 없는 경우") {
                it("ProjectNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectNotFoundException> { projectService.getProject(USER_ID, DELETED) }
                }
            }

            context("프로젝트에 접근 권한이 없는 경우") {
                it("NonAccessibleProjectException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectException> { projectService.getProject(USER_ID, NON_ACCESSIBLE) }
                }
            }
        }

        this.describe("getProjectList 호출될 때") {
            context("정상적인 경우") {
                it("모든 프로젝트가 SimpleResponse의 형태로 PageResponse에 담겨 반환된다") {
                    val response = shouldNotThrowAny { projectService.getProjectList(0, 10) }
                    response.content shouldContain SimpleResponse(project)
                }
            }
        }

        this.describe("getUserProject 호출될 때") {
            context("정상적인 경우") {
                it("유저의 프로젝트 목록이 SimpleResponse의 형태로 ListResponse에 담겨 반환된다") {
                    val response = shouldNotThrowAny { projectService.getUserProject(USER_ID) }
                    response.data shouldContain SimpleResponse(project)
                }
            }
        }

        this.describe("createProject 호출될 때") {
            val request = CreateRequest("create-project")

            context("정상적인 경우") {
                it("프로젝트가 DetailedResponse로 반환된다") {
                    val response = shouldNotThrowAny { projectService.createProject(USER_ID, request) }
                    response shouldBe DetailedResponse(project)
                }
            }
        }

        this.describe("patchProject 호출될 때") {
            val request = PatchRequest("patch-project")

            context("정상적인 경우") {
                it("프로젝트가 DetailedResponse로 반환된다") {
                    val response = shouldNotThrowAny { projectService.patchProject(USER_ID, NORMAL, request) }
                    response shouldBe DetailedResponse(project)
                }
            }

            context("프로젝트가 삭제되거나 없는 경우") {
                it("ProjectNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectNotFoundException> { projectService.patchProject(USER_ID, DELETED, request) }
                }
            }

            context("프로젝트에 접근 권한이 없는 경우") {
                it("NonAccessibleProjectException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectException> { projectService.patchProject(USER_ID, NON_ACCESSIBLE, request) }
                }
            }
        }

        this.describe("deleteProject 호출될 때") {
            context("정상적인 경우") {
                it("아무런 예외도 발생하지 않으며 리턴 값도 없다") {
                    val ret = shouldNotThrowAny { projectService.deleteProject(USER_ID, NORMAL) }
                    ret shouldBe Unit
                }
            }

            context("프로젝트가 삭제되거나 없는 경우") {
                it("ProjectNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectNotFoundException> { projectService.deleteProject(USER_ID, DELETED) }
                }
            }

            context("프로젝트에 접근 권한이 없는 경우") {
                it("NonAccessibleProjectException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectException> { projectService.deleteProject(USER_ID, NON_ACCESSIBLE) }
                }
            }
        }
    }

}