package com.wafflestudio.webgam.domain.page.repository

import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.config.TestQueryDslConfig
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
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
@DisplayName("ProjectPageRepository Data JPA 테스트")
class ProjectPageRepositoryImplTest(
    @Autowired private val userRepository: UserRepository,
    @Autowired private val projectRepository: ProjectRepository,
    @Autowired private val projectPageRepository: ProjectPageRepository,
) : DescribeSpec() {

    private lateinit var projects: List<Project>
    private lateinit var pageLists: List<List<ProjectPage>>

    companion object {
        val projectTitles = listOf("test-project-0", "test-project-1", "test-project-2", "deleted-project")
        val pageNames = listOf("test-page-0", "test-page-1", "test-page-2", "deleted-page")
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
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            projectPageRepository.deleteAll()
            projectRepository.deleteAll()
            userRepository.deleteAll()
        }
    }

    init {
        this.describe("findUndeletedProjectPageById 호출될 때") {
            context("성공적인 경우") {
                val id = pageLists[0][0].id

                it("삭제 되지 않은 ProjectPage가 반환된다") {
                    val projectPage = projectPageRepository.findUndeletedProjectPageById(id)

                    projectPage shouldNotBe null
                    projectPage!!.isDeleted shouldBe false
                }
            }

            context("해당 id를 갖는 ProjectPage가 없는 경우") {
                val id = pageLists[0][0].id + 1000

                it("NULL이 반환된다") {
                    val projectPage = projectPageRepository.findUndeletedProjectPageById(id)

                    projectPage shouldBe  null
                }
            }

            context("해당 id를 갖는 ProjectPage가 삭제된 경우") {
                val id = pageLists[0].find { it.isDeleted }!!.id

                it("NULL이 반환된다") {
                    val projectPage = projectPageRepository.findUndeletedProjectPageById(id)

                    projectPage shouldBe  null
                }
            }
        }
    }
}
