package com.wafflestudio.webgam.domain.`object`.repository

import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto.CreateRequest
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.config.TestQueryDslConfig
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@DataJpaTest
@Import(TestQueryDslConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Tag("Repository-Test")
@DisplayName("PageObjectRepository Data JPA 테스트")
class PageObjectRepositoryImplTest(
    @Autowired private val userRepository: UserRepository,
    @Autowired private val projectRepository: ProjectRepository,
    @Autowired private val projectPageRepository: ProjectPageRepository,
    @Autowired private val pageObjectRepository: PageObjectRepository,
) : DescribeSpec() {

    private lateinit var projects: List<Project>
    private lateinit var pageLists: List<List<ProjectPage>>
    private lateinit var pageObjectLists: List<List<PageObject>>

    companion object {
        val projectTitles = listOf("test-project-0", "test-project-1", "test-project-2", "deleted-project")
        val pageNames = listOf("test-page-0", "test-page-1", "test-page-2", "deleted-page")
        val objectNames = listOf("object-0", "object-1", "object-2", "object-3", "deleted-object-0", "deleted-object-1")
    }

    @Transactional
    override suspend fun beforeSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            val user = userRepository.save(User("test-id", "test-username", "test@email.com", ""))
            projects = projectTitles.map {
                val project = Project(user, it)
                if (it.contains("deleted")) project.delete()
                projectRepository.save(project)
            }
            pageLists = projects.map { p -> pageNames.map {
                val projectPage = ProjectPage(p, it)
                if (it.contains("deleted")) projectPage.delete()
                projectPageRepository.save(projectPage)
            } }
            pageObjectLists = pageLists.map { it.map { page -> objectNames.map { name ->
                val pageObject = PageObject(page, buildRequest(page.id, name))
                if (name.contains("deleted")) pageObject.delete()
                pageObjectRepository.save(pageObject)
            } }.flatten() }
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

    fun buildRequest(pageId: Long, objectName: String): CreateRequest = CreateRequest(
        pageId, objectName, PageObjectType.DEFAULT, 0, 0, 0, 0, 0, null, null, null
    )

    init {
        this.describe("findUndeletedPageObjectById 호출될 때") {
            context("성공적인 경우") {
                val id = pageObjectLists[0][0].id

                it("삭제 되지 않은 PageObject가 반환된다") {
                    val pageObject = pageObjectRepository.findUndeletedPageObjectById(id)

                    pageObject shouldNotBe null
                    pageObject!!.isDeleted shouldBe false
                }
            }

            context("해당 id를 갖는 PageObject가 없는 경우") {
                val id = pageObjectLists[0][0].id + 1000

                it("NULL이 반환된다") {
                    val pageObject = pageObjectRepository.findUndeletedPageObjectById(id)

                    pageObject shouldBe null
                }
            }

            context("해당 id를 갖는 PageObject가 삭제된 경우") {
                val id = pageObjectLists[0].find { it.isDeleted }!!.id

                it("NULL이 반환된다") {
                    val pageObject = pageObjectRepository.findUndeletedPageObjectById(id)

                    pageObject shouldBe null
                }
            }
        }

        this.describe("findAllUndeletedPageObjectsInProject 호출될 때") {
            context("Project ID를 인자로 넣으면") {
                val project = projects[1]
                val projectObjects = pageObjectLists[1]
                val otherProjectObjects = pageObjectLists[2]
                val deletedProject = projects[3]
                val deletedProjectObjects = pageObjectLists[3]

                it("해당 프로젝트의 모든 삭제 되지 않은 PageObject들이 반환된다") {
                    val findObjects = pageObjectRepository.findAllUndeletedPageObjectsInProject(project.id)
                    val expectedObjectCount = projectObjects.count { !it.isDeleted && !it.page.isDeleted }

                    findObjects.size shouldBe expectedObjectCount
                    findObjects.forAll { it.isDeleted shouldBe false }
                }

                it("해당 프로젝트의 삭제된 Page 에 속하는 PageObject는 반환되지 않는다") {
                    val findObjects = pageObjectRepository.findAllUndeletedPageObjectsInProject(project.id)
                    val objectsInDeletedPage = projectObjects.filter { it.page.name.contains("deleted") }

                    findObjects shouldNotContainAnyOf objectsInDeletedPage
                }

                it("다른 프로젝트의 PageObject는 반환되지 않는다") {
                    val findObjects = pageObjectRepository.findAllUndeletedPageObjectsInProject(project.id)

                    findObjects shouldNotContainAnyOf otherProjectObjects
                    findObjects shouldNotContainAnyOf deletedProjectObjects
                }

                it("삭제된 프로젝트인 경우 빈 List가 반환된다") {
                    val findObjects = pageObjectRepository.findAllUndeletedPageObjectsInProject(deletedProject.id)

                    findObjects.size shouldBe 0
                }
            }
        }
    }
}
