package com.wafflestudio.webgam.domain.project.service

import com.wafflestudio.webgam.TestUtils.Companion.testData1
import com.wafflestudio.webgam.domain.event.repository.ObjectEventRepository
import com.wafflestudio.webgam.domain.`object`.repository.PageObjectRepository
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import com.wafflestudio.webgam.domain.project.dto.ProjectDto.*
import com.wafflestudio.webgam.domain.project.exception.NonAccessibleProjectException
import com.wafflestudio.webgam.domain.project.exception.ProjectNotFoundException
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.config.TestQueryDslConfig
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringTestExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@Import(TestQueryDslConfig::class)
@ActiveProfiles("test")
@DisplayName("Project Service-Repository 테스트")
class ProjectServiceTestWithRepository(
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val projectPageRepository: ProjectPageRepository,
    private val pageObjectRepository: PageObjectRepository,
    private val objectEventRepository: ObjectEventRepository,
): BehaviorSpec() {

    private val projectService = ProjectService(projectRepository, userRepository)
    override fun extensions() = listOf(SpringTestExtension(SpringTestLifecycleMode.Root))
    override fun isolationMode() = IsolationMode.InstancePerLeaf

    override suspend fun afterSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            userRepository.deleteAll()
        }
    }

    init {
        this.Given("testData1") {
            val data = testData1()
            userRepository.saveAll(data)

            val undeletedProjects = data.map { it.projects }.flatten().filter { !it.isDeleted }
            val deletedProjects = data.map { it.projects }.flatten().filter { !it.isDeleted }
            When("프로젝트 전체 목록을 조회하면") {
                val response = shouldNotThrowAny { projectService.getProjectList(0, 20) }

                Then("삭제되지 않은 프로젝트들이 목록으로 조회된다") {
                    response.content shouldContainExactlyInAnyOrder undeletedProjects.map { SimpleResponse(it) }
                    response.numberOfElements shouldBe undeletedProjects.size
                }
                Then("삭제된 프로젝트들은 목록에 포함되지 않는다") {
                    if (deletedProjects.isNotEmpty())
                        response.content shouldNotContainAnyOf deletedProjects
                }
            }

            data.filter { !it.isDeleted }.forEach { user ->
                When("유저 ID로 프로젝트로 목록을 조회하면: ${user.username}") {
                    val undeletedUserProjects = user.projects.filter { !it.isDeleted }
                    val deletedUserProjects = user.projects.filter { it.isDeleted }
                    val response = shouldNotThrowAny { projectService.getUserProject(user.id) }

                    Then("삭제되지 않은 프로젝트들이 목록으로 조회된다") {
                        response.data shouldContainExactlyInAnyOrder undeletedUserProjects.map { SimpleResponse(it) }
                        response.count shouldBe undeletedUserProjects.size
                    }
                    Then("삭제된 프로젝트들은 목록에 포함되지 않는다") {
                        if (deletedUserProjects.isNotEmpty())
                            response.data shouldNotContainAnyOf deletedUserProjects
                    }
                }
            }

            data.filter { !it.isDeleted }.forEach { user ->
                val normalUserProjects = user.projects.filter { !it.isDeleted }
                val deletedUserProjects = user.projects.filter { it.isDeleted }
                val nonAccessibleProjects = data.filter { it.id != user.id }.map { it.projects }.flatten().filter { !it.isDeleted }

                normalUserProjects.forEach { project ->
                    When("일반적으로 유저 ID와 프로젝트 ID로 조회하면") {
                        val foundProject = projectRepository.findUndeletedProjectById(project.id)
                        val response = shouldNotThrowAny { projectService.getProject(user.id, project.id) }

                        Then("저장된 프로젝트가 조회된다") {
                            foundProject shouldBe project
                            response shouldBe DetailedResponse(project)
                        }
                    }
                }

                deletedUserProjects.forEach { project ->
                    When("삭제된 프로젝트 접근하면: ${user.username} ${project.title}") {
                        Then("ProjectNotFoundException 예외가 발생한다") {
                            shouldThrow<ProjectNotFoundException> {
                                projectService.getProject(user.id, project.id)
                            }
                            shouldThrow<ProjectNotFoundException> {
                                projectService.patchProject(user.id, project.id, PatchRequest("patch-project"))
                            }
                            shouldThrow<ProjectNotFoundException> {
                                projectService.deleteProject(user.id, project.id)
                            }
                        }
                    }
                }

                nonAccessibleProjects.forEach { project ->
                    When("접근권한이 없는 페이지에 접근하면: ${user.username} ${project.title}") {
                        Then("NonAccessibleProjectException 예외가 발생한다") {
                            shouldThrow<NonAccessibleProjectException> {
                                projectService.getProject(user.id, project.id)
                            }
                            shouldThrow<NonAccessibleProjectException> {
                                projectService.patchProject(user.id, project.id, PatchRequest("patch-project"))
                            }
                            shouldThrow<NonAccessibleProjectException> {
                                projectService.deleteProject(user.id, project.id)
                            }
                        }
                    }
                }
            }

            When("프로젝트를 생성하면") {
                val user = data.first { it.username == "user-01" }
                val request = CreateRequest("create-project")
                val response = shouldNotThrowAny { projectService.createProject(user.id, request) }
                val foundProject = projectRepository.findByIdOrNull(response.id)!!

                Then("정상적으로 DB에 저장된다") {
                    response shouldBe DetailedResponse(foundProject)
                    response.title shouldBe request.title
                }

                Then("성공적으로 연관관계 매핑이 이루어진다") {
                    user.projects shouldContain foundProject
                }
            }

            When("프로젝트를 수정하면") {
                val user = data.first { it.username == "user-01" }
                val project = user.projects.first { it.title == "project-01" }
                val request = PatchRequest("patch-project")
                val response = shouldNotThrowAny { projectService.patchProject(user.id, project.id, request) }
                val foundProject = projectRepository.findByIdOrNull(project.id)!!

                Then("정상적으로 DB에 반영된다") {
                    foundProject shouldBe project
                    response shouldBe DetailedResponse(foundProject)
                    response.title shouldBe request.title
                }
            }

            When("프로젝트를 삭제하면") {
                val user = data.first { it.username == "user-01" }
                val project = user.projects.first { it.title == "project-01" }
                shouldNotThrowAny { projectService.deleteProject(user.id, project.id) }
                val foundProject = projectRepository.findByIdOrNull(project.id)!!

                Then("정상적으로 DB에 반영된다") {
                    foundProject shouldBe project
                    foundProject.isDeleted shouldBe true
                }

                Then("프로젝트 조회, 수정, 삭제가 더 이상 불가능하다") {
                    shouldThrow<ProjectNotFoundException> { projectService.getProject(user.id, project.id) }
                    shouldThrow<ProjectNotFoundException> { projectService.patchProject(user.id, project.id, PatchRequest("patch-project")) }
                    shouldThrow<ProjectNotFoundException> { projectService.deleteProject(user.id, project.id) }
                }

                Then("하위 페이지, 오브젝트, 이벤트들도 삭제된다") {
                    project.pages.forEach { page ->
                        val foundPage = projectPageRepository.findByIdOrNull(page.id)!!

                        foundPage shouldBe page
                        foundPage.isDeleted shouldBe true

                        page.objects.forEach { pageObject ->
                            val foundObject = pageObjectRepository.findByIdOrNull(pageObject.id)!!

                            foundObject shouldBe pageObject
                            foundObject.isDeleted shouldBe true

                            pageObject.events.forEach { event ->
                                val foundEvent = objectEventRepository.findByIdOrNull(event.id)!!

                                foundEvent shouldBe event
                                foundEvent.isDeleted shouldBe true
                            }
                        }
                    }
                }

                Then("모든 프로젝트 목록 조회에서도 제외된다") {
                    val response = shouldNotThrowAny { projectService.getProjectList(0, 20) }
                    response.content shouldNotContain SimpleResponse(project)
                }

                Then("유저의 프로젝트 목록 조회에서도 제외된다") {
                    val response = shouldNotThrowAny { projectService.getUserProject(user.id) }
                    response.data shouldNotContain SimpleResponse(project)
                }
            }
        }
    }
}