package com.wafflestudio.webgam.domain.event.repository

import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import com.wafflestudio.webgam.domain.event.model.TransitionType
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType.DEFAULT
import com.wafflestudio.webgam.domain.`object`.repository.PageObjectRepository
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
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

@DataJpaTest
@Import(TestQueryDslConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Tag("Repository-Test")
@DisplayName("ObjectEventRepository Data JPA 테스트")
class ObjectEventRepositoryImplTest(
    @Autowired private val userRepository: UserRepository,
    @Autowired private val projectRepository: ProjectRepository,
    @Autowired private val projectPageRepository: ProjectPageRepository,
    @Autowired private val pageObjectRepository: PageObjectRepository,
    @Autowired private val objectEventRepository: ObjectEventRepository,
): DescribeSpec() {

    private val user = userRepository.save(User("test-id", "test-username", "test@email.com", ""))
    private val project = projectRepository.save(Project(user, "test-project"))
    private val page = projectPageRepository.save(ProjectPage(project, "test-page"))
    private val pageObject = pageObjectRepository.save(PageObject(page, "test-object", DEFAULT, 0, 0, 0, 0, 0, null, null, null, null))
    private val event = objectEventRepository.save(ObjectEvent(pageObject, null, TransitionType.DEFAULT))
    private val deletedEvent = objectEventRepository.save(ObjectEvent(pageObject, null, TransitionType.DEFAULT))

    override suspend fun beforeSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            deletedEvent.delete()
            objectEventRepository.save(deletedEvent)
        }
    }

    init {
        this.describe("findUndeletedObjectEventById 호출될 때") {
            context("성공적인 경우") {
                val id = event.id
                val foundEvent = objectEventRepository.findUndeletedObjectEventById(id)

                it ("삭제 되지 않은 ObjectEvent가 반환된다") {
                    foundEvent shouldNotBe null
                    foundEvent!!.isDeleted shouldBe false
                }
            }

            context("해당 id를 갖는 PageObject가 없는 경우") {
                val id = deletedEvent.id + 1000
                val foundEvent = objectEventRepository.findUndeletedObjectEventById(id)

                it ("NULL이 반환된다") {
                    foundEvent shouldBe null
                }
            }

            context("해당 id를 갖는 PageObject가 삭제 된 경우") {
                val id = deletedEvent.id
                val foundEvent = objectEventRepository.findUndeletedObjectEventById(id)

                it ("NULL이 반환된다") {
                    foundEvent shouldBe null
                }
            }
        }
    }
}