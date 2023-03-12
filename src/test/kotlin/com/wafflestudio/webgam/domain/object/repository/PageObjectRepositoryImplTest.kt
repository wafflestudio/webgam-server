package com.wafflestudio.webgam.domain.`object`.repository

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
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@Import(TestQueryDslConfig::class)
@ActiveProfiles("test")
@DisplayName("PageObjectRepository 테스트")
class PageObjectRepositoryImplTest(
    private val userRepository: UserRepository,
    private val pageObjectRepository: PageObjectRepository,
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
            val objects = projects.map { p -> p.pages.map { it.objects }.flatten() }.flatten()
            val maxId = pageObjectRepository.findAll().maxOf { it.id }

            When("findUndeletedPageObjectById 호출하면") {
                for (pageObject in objects) {
                    val foundObject = pageObjectRepository.findUndeletedPageObjectById(pageObject.id)
                    if (!pageObject.isDeleted) {
                        Then("삭제되지 않은 오브젝트 객체를 반환한다: ${pageObject.name}") { foundObject shouldBe pageObject }
                    } else {
                        Then("삭제 되었으면 NULL을 반환한다: ${pageObject.name}") { foundObject shouldBe null }
                    }
                }

                val foundObject = pageObjectRepository.findUndeletedPageObjectById(maxId + 1)
                Then("존재하지 않는 경우에도 NULL을 반환한다") { foundObject shouldBe null }
            }

            When("findAllUndeletedPageObjectsInProject 호출하면") {
                for (project in projects) {
                    val projectObjects = project.pages.map { it.objects }.flatten()
                    val foundObjects = pageObjectRepository.findAllUndeletedPageObjectsInProject(project.id)

                    Then("해당 프로젝트의 모든 삭제되지 않은 오브젝트들을 반환한다: ${project.title}") {
                        foundObjects shouldContainExactlyInAnyOrder projectObjects.filter { !it.isDeleted }
                    }

                    val deletedPageObjects = project.pages.filter { it.isDeleted }.map { it.objects }.flatten()
                    Then("삭제된 페이지의 오브젝트들은 반환하지 않는다: ${project.title}") {
                        if (deletedPageObjects.isNotEmpty())
                            foundObjects shouldNotContainAnyOf deletedPageObjects
                    }

                    val otherProjects = data.map { u -> u.projects.filter { it.id != project.id } }.flatten()
                    val otherProjectObjects = otherProjects.map { p -> p.pages.map { it.objects }.flatten() }.flatten()
                    Then("다른 프로젝트의 오브젝트들은 반환하지 않는다: ${project.title}") {
                        foundObjects shouldNotContainAnyOf otherProjectObjects
                    }

                    if (project.isDeleted) {
                        Then("삭제된 프로젝트의 경우, 빈 리스트를 반환한다: ${project.title}") {
                            foundObjects.shouldBeEmpty()
                        }
                    }
                }
            }
        }
    }
}
