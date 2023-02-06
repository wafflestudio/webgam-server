package com.wafflestudio.webgam.domain.project.repository

import com.wafflestudio.webgam.domain.project.model.Project
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
@DisplayName("ProjectRepository Data JPA 테스트")
class ProjectRepositoryImplTest(
    @Autowired private val userRepository: UserRepository,
    @Autowired private val projectRepository: ProjectRepository,
) : DescribeSpec() {

    private lateinit var projects: List<Project>

    companion object {
        val projectTitles = listOf("test-project-0", "test-project-1", "test-project-2", "deleted-project")
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
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            projectRepository.deleteAll()
            userRepository.deleteAll()
        }
    }

    init {
        this.describe("findUndeletedProjectById 호출될 때") {
            context("성공적인 경우") {
                val id = projects[0].id

                it("삭제 되지 않은 Project가 반환된다") {
                    val project = projectRepository.findUndeletedProjectById(id)

                    project shouldNotBe null
                    project!!.isDeleted shouldBe false
                }
            }

            context("해당 id를 갖는 Project가 없는 경우") {
                val id = projects[0].id + 1000

                it("NULL이 반환된다") {
                    val project = projectRepository.findUndeletedProjectById(id)

                    project shouldBe  null
                }
            }

            context("해당 id를 갖는 Project가 삭제된 경우") {
                val id = projects.find { it.isDeleted }!!.id

                it("NULL이 반환된다") {
                    val project = projectRepository.findUndeletedProjectById(id)

                    project shouldBe  null
                }
            }
        }
    }
}
