package com.wafflestudio.webgam.domain.page.service

import com.wafflestudio.webgam.TestUtils.Companion.testData1
import com.wafflestudio.webgam.domain.event.repository.ObjectEventRepository
import com.wafflestudio.webgam.domain.`object`.repository.PageObjectRepository
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto.*
import com.wafflestudio.webgam.domain.page.exception.NonAccessibleProjectPageException
import com.wafflestudio.webgam.domain.page.exception.ProjectPageNotFoundException
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
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
@DisplayName("ProjectPage Service-Repository 테스트")
class ProjectPageServiceTestWithRepository(
    private val userRepository: UserRepository,
    projectRepository: ProjectRepository,
    private val projectPageRepository: ProjectPageRepository,
    private val pageObjectRepository: PageObjectRepository,
    private val objectEventRepository: ObjectEventRepository,
): BehaviorSpec() {

    private val projectPageService = ProjectPageService(projectPageRepository, projectRepository)
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

            data.filter { !it.isDeleted }.forEach { user ->
                val normalUserPages = user.projects.map { it.pages }.flatten().filter { !it.isDeleted }
                val deletedUserPages = user.projects.map { it.pages }.flatten().filter { it.isDeleted }
                val nonAccessiblePages = data.filter { it.id != user.id }.map { u -> u.projects.map { it.pages }
                    .flatten() }.flatten().filter { !it.isDeleted }

                normalUserPages.forEach { page ->
                    When("일반적으로 유저 ID와 페이지 ID로 페이지 조회하면: ${user.username} ${page.name}") {
                        val foundPage = projectPageRepository.findUndeletedProjectPageById(page.id)
                        val response = shouldNotThrowAny { projectPageService.getProjectPage(user.id, page.id) }
                        Then("저장된 페이지가 조회된다") {
                            foundPage shouldBe page
                            response shouldBe DetailedResponse(page)
                        }
                    }
                }

                deletedUserPages.forEach { page ->
                    When("삭제된 페이지에 접근하면: ${user.username} ${page.name}") {
                        Then("ProjectPageNotFoundException 예외가 발생한다") {
                            shouldThrow<ProjectPageNotFoundException> {
                                projectPageService.getProjectPage(user.id, page.id)
                            }
                            shouldThrow<ProjectPageNotFoundException> {
                                projectPageService.patchProjectPage(user.id, page.id, PatchRequest("patch"))
                            }
                            shouldThrow<ProjectPageNotFoundException> {
                                projectPageService.deleteProjectPage(user.id, page.id)
                            }
                        }
                    }
                }

                nonAccessiblePages.forEach { page ->
                    When("접근권한이 없는 페이지에 접근하면: ${user.username} ${page.name}") {
                        Then("NonAccessibleProjectPageException 예외가 발생한다") {
                            shouldThrow<NonAccessibleProjectPageException> {
                                projectPageService.getProjectPage(user.id, page.id)
                            }
                            shouldThrow<NonAccessibleProjectPageException> {
                                projectPageService.patchProjectPage(user.id, page.id, PatchRequest("patch"))
                            }
                            shouldThrow<NonAccessibleProjectPageException> {
                                projectPageService.deleteProjectPage(user.id, page.id)
                            }
                        }
                    }
                }
            }

            When("새로운 페이지를 생성하면") {
                val user = data.first { it.username == "user-01" }
                val project = user.projects.first { it.title == "project-01" }
                val request = CreateRequest(project.id, "new-page")
                val response = shouldNotThrowAny { projectPageService.createProjectPage(user.id, request) }
                val foundPage = projectPageRepository.findByIdOrNull(response.id)!!

                Then("정상적으로 DB에 저장된다") {
                    response shouldBe DetailedResponse(foundPage)
                    response.projectId shouldBe project.id
                    response.name shouldBe request.name
                }

                Then("성공적으로 연관관계 매핑이 이루어진다") {
                    project.pages shouldContain foundPage
                }
            }

            When("페이지를 수정하면") {
                val user = data.first { it.username == "user-01" }
                val project = user.projects.first { it.title == "project-01" }
                val page = project.pages.first { it.name == "page-02" }
                val request = PatchRequest("modified-page")
                val response = shouldNotThrowAny { projectPageService.patchProjectPage(user.id, page.id, request) }
                val foundPage = projectPageRepository.findByIdOrNull(page.id)!!

                Then("정상적으로 DB에 반영된다") {
                    foundPage shouldBe page
                    response shouldBe DetailedResponse(foundPage)
                    response.name shouldBe request.name
                }
            }

            When("페이지를 삭제하면") {
                val user = data.first { it.username == "user-01" }
                val project = user.projects.first { it.title == "project-01" }
                val page = project.pages.first { it.name == "page-02" }
                shouldNotThrowAny { projectPageService.deleteProjectPage(user.id, page.id) }
                val foundPage = projectPageRepository.findByIdOrNull(page.id)!!

                Then("정상적으로 DB에 반영된다") {
                    foundPage shouldBe page
                    foundPage.isDeleted shouldBe true
                }

                Then("페이지 조회, 수정, 삭제가 더 이상 불가능하다") {
                    shouldThrow<ProjectPageNotFoundException> { projectPageService.getProjectPage(user.id, page.id) }
                    shouldThrow<ProjectPageNotFoundException> { projectPageService.patchProjectPage(user.id, page.id, PatchRequest("patch")) }
                    shouldThrow<ProjectPageNotFoundException> { projectPageService.deleteProjectPage(user.id, page.id) }
                }

                Then("하위 오브젝트, 이벤트들도 삭제된다") {
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
        }
    }
}