package com.wafflestudio.webgam.domain.page.repository

import com.wafflestudio.webgam.TestUtils.Companion.testData1
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.config.TestQueryDslConfig
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringTestExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@Import(TestQueryDslConfig::class)
@ActiveProfiles("test")
@DisplayName("ProjectPageRepository 테스트")
class ProjectPageRepositoryImplTest(
    private val userRepository: UserRepository,
    private val projectPageRepository: ProjectPageRepository,
) : BehaviorSpec() {

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

            val pages = data.map { u -> u.projects.map { it.pages }.flatten() }.flatten()
            val maxId = projectPageRepository.findAll().maxOf { it.id }

            When("findUndeletedProjectPageById 호출하면") {
                for (page in pages) {
                    val foundPage = projectPageRepository.findUndeletedProjectPageById(page.id)
                    if (!page.isDeleted) {
                        Then("삭제 되지 않은 페이지 객체를 반환한다: ${page.name}") { foundPage shouldBe page }
                    } else {
                        Then("삭제 되었으면 NULL을 반환한다: ${page.name}") { foundPage shouldBe null }
                    }
                }

                val foundPage = projectPageRepository.findUndeletedProjectPageById(maxId + 1)
                Then("존재하지 않는 경우에도 NULL을 반환한다") { foundPage shouldBe null }
            }
        }
    }
}
