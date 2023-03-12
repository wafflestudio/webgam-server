package com.wafflestudio.webgam.domain.event.repository

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
@DisplayName("ObjectEventRepository 테스트")
class ObjectEventRepositoryImplTest(
    private val userRepository: UserRepository,
    private val objectEventRepository: ObjectEventRepository,
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

            val events = data.map { u -> u.projects.map { p -> p.pages.map { pp -> pp.objects.map { it.events }
                .flatten() }.flatten() }.flatten() }.flatten()
            val maxId = objectEventRepository.findAll().maxOf { it.id }

            When("findUndeletedObjectEventById 호출하면") {
                for (event in events) {
                    val foundEvent = objectEventRepository.findUndeletedObjectEventById(event.id)
                    if (!event.isDeleted) {
                        Then("삭제되지 않은 이벤트 객체를 반환한다: ${event.id}") { foundEvent shouldBe event }
                    } else {
                        Then("삭제 되었으면 NULL을 반환한다: ${event.id}") { foundEvent shouldBe null }
                    }
                }

                val foundEvent = objectEventRepository.findUndeletedObjectEventById(maxId + 1)
                Then("존재하지 않은 경우에도 NULL을 반환한다") { foundEvent shouldBe null }
            }
        }
    }
}