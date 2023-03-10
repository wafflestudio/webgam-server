package com.wafflestudio.webgam.domain.project.repository

import com.wafflestudio.webgam.TestUtils.Companion.testData1
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.config.TestQueryDslConfig
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringTestExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@Import(TestQueryDslConfig::class)
@ActiveProfiles("test")
@DisplayName("ProjectRepository 테스트")
class ProjectRepositoryImplTest(
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
): BehaviorSpec() {
    override fun extensions() = listOf(SpringTestExtension(SpringTestLifecycleMode.Root))

    override suspend fun afterSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            userRepository.deleteAll()
        }
    }

    init {
        this.Given("testData1") {
            val data = testData1()
            userRepository.saveAll(data)

            val projects = data.map { it.projects }.flatten()
            val maxId = projectRepository.findAll().maxOf { it.id }
            val maxUserId = userRepository.findAll().maxOf { it.id }

            When("findUndeletedProjectById 호출하면") {
                for (project in projects) {
                    val foundProject = projectRepository.findUndeletedProjectById(project.id)
                    if (!project.isDeleted) {
                        Then("삭제 되지 않은 프로젝트 객체를 반환한다: ${project.title}") { foundProject shouldBe project }
                    } else {
                        Then("삭제 되었으면 NULL을 반환한다: ${project.title}") { foundProject shouldBe null }
                    }
                }

                val foundProject = projectRepository.findUndeletedProjectById(maxId + 1)
                Then("존재하지 않는 경우에도 NULL을 반환한다") { foundProject shouldBe null }
            }

            When("findUndeletedAllByOwnerIdEquals 호출하면") {
                for (user in data) {
                    val foundProjects = projectRepository.findUndeletedAllByOwnerIdEquals(user.id)
                    Then("유저의 삭제되지 않은 모든 프로젝트가 리스트로 반환된다: ${user.username}") {
                        foundProjects shouldContainExactlyInAnyOrder user.projects.filter { !it.isDeleted }
                    }
                }

                val foundProjects = projectRepository.findUndeletedAllByOwnerIdEquals(maxUserId + 1)
                Then("해당 유저가 없는 경우에는 빈 리스트를 반환한다") { foundProjects.shouldBeEmpty() }
            }

            When("findUndeletedAll 호출하면") {
                val foundProjects = projectRepository.findUndeletedAll(PageRequest.of(0, projects.size))
                Then("삭제되지 않은 프로젝트가 리스트로 반환된다") {
                    foundProjects.content shouldContainExactlyInAnyOrder projects.filter { !it.isDeleted }
                }
            }
        }
    }
}
