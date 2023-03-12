package com.wafflestudio.webgam.domain.event.service

import com.wafflestudio.webgam.TestUtils.Companion.testData1
import com.wafflestudio.webgam.domain.event.dto.ObjectEventDto.*
import com.wafflestudio.webgam.domain.event.exception.NonAccessibleObjectEventException
import com.wafflestudio.webgam.domain.event.exception.ObjectEventNotFoundException
import com.wafflestudio.webgam.domain.event.model.TransitionType
import com.wafflestudio.webgam.domain.event.repository.ObjectEventRepository
import com.wafflestudio.webgam.domain.`object`.repository.PageObjectRepository
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
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
import io.kotest.matchers.collections.shouldNotContain
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
@DisplayName("ObjectEvent Service-Repository 테스트")
class ObjectEventServiceTestWithRepository(
    private val userRepository: UserRepository,
    projectPageRepository: ProjectPageRepository,
    pageObjectEventRepository: PageObjectRepository,
    private val objectEventRepository: ObjectEventRepository,
): BehaviorSpec() {

    private val objectEventService = ObjectEventService(projectPageRepository, pageObjectEventRepository, objectEventRepository)
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
                val userEvents = user.projects.map { p -> p.pages.map { pp -> pp.objects.map { it.events }
                    .flatten() }.flatten() }.flatten()
                val normalEvents = userEvents.filter { !it.isDeleted }
                val deletedEvents = userEvents.filter { it.isDeleted }
                val nonAccessibleEvents = data.filter { it.id != user.id }
                    .map { u -> u.projects.map { p -> p.pages.map { pp -> pp.objects.map { it.events }
                        .flatten() }.flatten() }.flatten() }.flatten().filter { !it.isDeleted }

                normalEvents.forEach { event ->
                    When("일반적으로 유저 ID와 이벤트 ID로 조회하면: ${user.username} ${event.`object`.name}") {
                        val foundEvent = objectEventRepository.findUndeletedObjectEventById(event.id)
                        val response = shouldNotThrowAny { objectEventService.getEvent(user.id, event.id) }
                        Then("저장된 이벤트가 조회된다") {
                            foundEvent shouldBe event
                            response shouldBe SimpleResponse(event)
                        }
                    }
                }

                deletedEvents.forEach { event ->
                    When("삭제된 이벤트 접근하면: ${user.username} ${event.`object`.name}") {
                        Then("ObjectEventNotFoundException 예외가 발생한다") {
                            shouldThrow<ObjectEventNotFoundException> { objectEventService.getEvent(user.id, event.id) }
                            shouldThrow<ObjectEventNotFoundException> { objectEventService.updateEvent(user.id, event.id, PatchRequest(null, null)) }
                            shouldThrow<ObjectEventNotFoundException> { objectEventService.deleteEvent(user.id, event.id) }
                        }
                    }
                }

                nonAccessibleEvents.forEach { event ->
                    When("접근 권한이 없는 이벤트에 접근하면: ${user.username} ${event.`object`.name}") {
                        Then("NonAccessibleObjectEventException 예외가 발생한다") {
                            shouldThrow<NonAccessibleObjectEventException> { objectEventService.getEvent(user.id, event.id) }
                            shouldThrow<NonAccessibleObjectEventException> { objectEventService.updateEvent(user.id, event.id, PatchRequest(null, null)) }
                            shouldThrow<NonAccessibleObjectEventException> { objectEventService.deleteEvent(user.id, event.id) }
                        }
                    }
                }
            }

            When("이벤트를 생성하면") {
                val user = data.first { it.username == "user-01" }
                val nextPage = user.projects.first { it.title == "project-01" }.pages.first { it.name == "page-01" }
                val pageObject = user.projects.first { it.title == "project-01" }.pages.first { it.name == "page-02" }
                    .objects.first { it.name == "object-02" }
                val request = CreateRequest(pageObject.id, TransitionType.DEFAULT, nextPage.id)
                val response = shouldNotThrowAny { objectEventService.createEvent(user.id, request) }
                val foundEvent = objectEventRepository.findByIdOrNull(response.id)!!

                Then("정상적으로 DB에 저장된다") {
                    response shouldBe SimpleResponse(foundEvent)
                    response.transitionType shouldBe request.transitionType
                    response.nextPage?.id shouldBe request.nextPageId
                }

                Then("성공적으로 연관관계 매핑이 이루어진다") {
                    pageObject.events shouldContain foundEvent
                    pageObject.event shouldBe foundEvent
                    nextPage.triggeredEvents shouldContain foundEvent
                }
            }

            When("이벤트를 수정하면") {
                val user = data.first { it.username == "user-01" }
                val nextPage = user.projects.first { it.title == "project-01" }.pages.first { it.name == "page-01" }
                val event = user.projects.first { it.title == "project-01" }.pages.first { it.name == "page-02" }
                    .objects.first { it.name == "object-03" }.events.first { !it.isDeleted }
                val request = PatchRequest(null, nextPage.id)

                nextPage.triggeredEvents shouldNotContain event
                val response = shouldNotThrowAny { objectEventService.updateEvent(user.id, event.id, request) }
                val foundEvent = objectEventRepository.findByIdOrNull(response.id)!!

                Then("정상적으로 DB에 저장된다") {
                    response shouldBe SimpleResponse(foundEvent)
                    response.nextPage?.id shouldBe request.nextPageId
                }

                Then("성공적으로 연관관계 매핑이 이루어진다") {
                    nextPage.triggeredEvents shouldContain foundEvent
                }
            }

            When("이벤트를 삭제하면") {
                val user = data.first { it.username == "user-01" }
                val event = user.projects.first { it.title == "project-01" }.pages.first { it.name == "page-02" }
                    .objects.first { it.name == "object-03" }.events.first { !it.isDeleted }
                shouldNotThrowAny { objectEventService.deleteEvent(user.id, event.id) }
                val foundEvent = objectEventRepository.findByIdOrNull(event.id)!!

                Then("정상적으로 DB에 저장된다") {
                    foundEvent shouldBe event
                    foundEvent.isDeleted shouldBe true
                }

                Then("이벤트 조회, 수정, 삭제가 더 이상 불가능하다") {
                    shouldThrow<ObjectEventNotFoundException> { objectEventService.getEvent(user.id, event.id) }
                    shouldThrow<ObjectEventNotFoundException> { objectEventService.updateEvent(user.id, event.id, PatchRequest(null, null)) }
                    shouldThrow<ObjectEventNotFoundException> { objectEventService.deleteEvent(user.id, event.id) }
                }
            }
        }
    }
}