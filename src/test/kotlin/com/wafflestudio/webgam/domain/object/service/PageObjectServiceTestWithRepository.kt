package com.wafflestudio.webgam.domain.`object`.service

import com.wafflestudio.webgam.TestUtils.Companion.makeFieldList
import com.wafflestudio.webgam.TestUtils.Companion.testData1
import com.wafflestudio.webgam.domain.event.repository.ObjectEventRepository
import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto.*
import com.wafflestudio.webgam.domain.`object`.exception.NonAccessiblePageObjectException
import com.wafflestudio.webgam.domain.`object`.exception.PageObjectNotFoundException
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType.IMAGE
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType.TEXT
import com.wafflestudio.webgam.domain.`object`.repository.PageObjectRepository
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import com.wafflestudio.webgam.domain.project.exception.ProjectNotFoundException
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
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldNotContainAnyOf
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
@DisplayName("PageObject Service-Repository ?????????")
class PageObjectServiceTestWithRepository(
    private val userRepository: UserRepository,
    projectRepository: ProjectRepository,
    projectPageRepository: ProjectPageRepository,
    private val pageObjectRepository: PageObjectRepository,
    private val objectEventRepository: ObjectEventRepository,
): BehaviorSpec() {

    private val pageObjectService = PageObjectService(projectRepository, projectPageRepository, pageObjectRepository)
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
                user.projects.filter { !it.isDeleted }.forEach { project ->
                    When("???????????? ??? ???????????? ?????? ????????? ????????????: ${user.username} ${project.title}") {
                        val normalObjects = project.pages.map { it.objects }.flatten().filter { !it.isDeleted && it.isAccessibleTo(user.id) }
                        val deletedObjects = project.pages.map { it.objects }.flatten().filter { it.isDeleted }
                        val nonAccessibleObjects = project.pages.map { it.objects }.flatten().filter { !it.isDeleted && !it.isAccessibleTo(user.id) }
                        val response = shouldNotThrowAny { pageObjectService.listProjectObjects(user.id, project.id) }

                        Then("???????????? ?????? ?????? ?????????????????? ???????????? ????????????") {
                            response.data shouldContainExactlyInAnyOrder normalObjects.map { DetailedResponse(it) }
                            response.count shouldBe normalObjects.size
                        }
                        Then("????????? ?????? ?????????????????? ????????? ???????????? ?????????") {
                            if (deletedObjects.isNotEmpty())
                                response.data shouldNotContainAnyOf deletedObjects.map { DetailedResponse(it) }
                        }
                        Then("?????? ???????????? ?????????????????? ????????? ???????????? ?????????") {
                            if (nonAccessibleObjects.isNotEmpty())
                                response.data shouldNotContainAnyOf nonAccessibleObjects.map { DetailedResponse(it) }
                        }
                    }
                }
            }

            data.filter { !it.isDeleted }.forEach { user ->
                user.projects.filter { it.isDeleted }.forEach { project ->
                    When("????????? ???????????? ??? ???????????? ?????? ????????? ????????????: ${user.username} ${project.title}") {
                        Then("ProjectNotFoundException ????????? ????????????") {
                            shouldThrow<ProjectNotFoundException> { pageObjectService.listProjectObjects(user.id, project.id) }
                        }
                    }
                }
            }

            data.filter { !it.isDeleted }.forEach { user ->
                val userObjects = user.projects.map { p -> p.pages.map { it.objects }.flatten() }.flatten()
                val normalObjects = userObjects.filter { !it.isDeleted }
                val deletedObjects = userObjects.filter { it.isDeleted }
                val nonAccessibleObjects = data.filter { it.id != user.id }.map { u -> u.projects.map { p -> p.pages.map { it.objects }
                    .flatten() }.flatten() }.flatten().filter { !it.isDeleted }

                normalObjects.forEach { pageObject ->
                    When("??????????????? ?????? ID??? ???????????? ID??? ????????????: ${user.username} ${pageObject.name}") {
                        val foundObject = pageObjectRepository.findUndeletedPageObjectById(pageObject.id)
                        val response = shouldNotThrowAny { pageObjectService.getObject(user.id, pageObject.id) }

                        Then("????????? ??????????????? ????????????") {
                            foundObject shouldBe pageObject
                            response shouldBe DetailedResponse(pageObject)
                        }
                    }
                }

                deletedObjects.forEach { pageObject ->
                    When("????????? ???????????? ????????????: ${user.username} ${pageObject.name}") {
                        Then("PageObjectNotFoundException ????????? ????????????") {
                            shouldThrow<PageObjectNotFoundException> {
                                pageObjectService.getObject(user.id, pageObject.id)
                            }
                            shouldThrow<PageObjectNotFoundException> {
                                val request = PatchRequest(null, null, null, null, null, null, null, null, null)
                                pageObjectService.modifyObject(user.id, pageObject.id, request)
                            }
                            shouldThrow<PageObjectNotFoundException> {
                                pageObjectService.deleteObject(user.id, pageObject.id)
                            }
                        }
                    }
                }

                nonAccessibleObjects.forEach { pageObject ->
                    When("?????? ????????? ?????? ??????????????? ????????????: ${user.username} ${pageObject.name}") {
                        Then("NonAccessiblePageObjectException ????????? ????????????") {
                            shouldThrow<NonAccessiblePageObjectException> {
                                pageObjectService.getObject(user.id, pageObject.id)
                            }
                            shouldThrow<NonAccessiblePageObjectException> {
                                val request = PatchRequest(null, null, null, null, null, null, null, null, null)
                                pageObjectService.modifyObject(user.id, pageObject.id, request)
                            }
                            shouldThrow<NonAccessiblePageObjectException> {
                                pageObjectService.deleteObject(user.id, pageObject.id)
                            }
                        }
                    }
                }
            }

            When("??????????????? ????????????") {
                val user = data.first { it.username == "user-01" }
                val page = user.projects.first { it.title == "project-01" }.pages.first { it.name == "page-01" }
                val request = CreateRequest(page.id, "create-object", TEXT, 25, 35, 10, -30, 1, "object-text", 16, null)
                val response = shouldNotThrowAny { pageObjectService.createObject(user.id, request) }
                val foundObject = pageObjectRepository.findByIdOrNull(response.id)!!

                Then("??????????????? DB??? ????????????") {
                    response shouldBe SimpleResponse(foundObject)
                    response.name shouldBe request.name
                    response.type shouldBe request.type
                    response.width shouldBe request.width
                    response.height shouldBe request.height
                    response.xPosition shouldBe request.xPosition
                    response.yPosition shouldBe request.yPosition
                    response.zIndex shouldBe request.zIndex
                    response.textContent shouldBe request.textContent
                    response.fontSize shouldBe request.fontSize
                    response.imageSource shouldBe request.imageSource
                }

                Then("??????????????? ???????????? ????????? ???????????????") {
                    page.objects shouldContain foundObject
                }
            }

            When("??????????????? ????????????") {
                val user = data.first { it.username == "user-01" }
                val pageObject = user.projects.first { it.title == "project-01" }.pages.first { it.name == "page-02" }.objects.first { it.name == "object-01" }
                val combinations = makeFieldList(
                    listOf(null, IMAGE, TEXT).map { it to null },
                    listOf(null, 11).map { it to null },
                    listOf(null, 22).map { it to null },
                    listOf(null, -80).map { it to null },
                    listOf(null, 28).map { it to null },
                    listOf(null, 8).map { it to null },
                    listOf(null, "patch-text").map { it to null },
                    listOf(null, 20).map { it to null },
                    listOf(null, "patch-image-source").map { it to null },
                )
                combinations.forAll { (l, _) ->
                    val request = PatchRequest(
                        l[0] as PageObjectType?,
                        l[1] as Int?,
                        l[2] as Int?,
                        l[3] as Int?,
                        l[4] as Int?,
                        l[5] as Int?,
                        l[6] as String?,
                        l[7] as Int?,
                        l[8] as String?
                    )
                    val beforeUpdate = DetailedResponse(pageObject)
                    val response = shouldNotThrowAny { pageObjectService.modifyObject(user.id, pageObject.id, request) }
                    val foundObject = pageObjectRepository.findByIdOrNull(pageObject.id)!!

                    Then("NULL??? ?????? ????????? ??????????????? DB??? ????????????") {
                        foundObject shouldBe pageObject
                        response shouldBe DetailedResponse(foundObject)
                        response.type shouldBe (request.type ?: beforeUpdate.type)
                        response.width shouldBe (request.width ?: beforeUpdate.width)
                        response.height shouldBe (request.height ?: beforeUpdate.height)
                        response.xPosition shouldBe (request.xPosition ?: beforeUpdate.xPosition)
                        response.yPosition shouldBe (request.yPosition ?: beforeUpdate.yPosition)
                        response.zIndex shouldBe (request.zIndex ?: beforeUpdate.zIndex)
                        response.textContent shouldBe (request.textContent ?: beforeUpdate.textContent)
                        response.fontSize shouldBe (request.fontSize ?: beforeUpdate.fontSize)
                        response.imageSource shouldBe (request.imageSource ?: beforeUpdate.imageSource)
                    }
                }
            }

            When("??????????????? ????????????") {
                val user = data.first { it.username == "user-01" }
                val pageObject = user.projects.first { it.title == "project-01" }.pages.first { it.name == "page-02" }.objects.first { it.name == "object-01" }
                shouldNotThrowAny { pageObjectService.deleteObject(user.id, pageObject.id) }
                val foundObject = pageObjectRepository.findByIdOrNull(pageObject.id)!!

                Then("??????????????? DB??? ????????????") {
                    foundObject shouldBe pageObject
                    foundObject.isDeleted shouldBe true
                }

                Then("???????????? ??????, ??????, ????????? ??? ?????? ???????????????") {
                    shouldThrow<PageObjectNotFoundException> { pageObjectService.getObject(user.id, pageObject.id) }
                    shouldThrow<PageObjectNotFoundException> { pageObjectService.modifyObject(user.id, pageObject.id,
                        PatchRequest(null, null, null, null, null, null, null, null, null)) }
                    shouldThrow<PageObjectNotFoundException> { pageObjectService.deleteObject(user.id, pageObject.id) }
                }

                Then("???????????? ??? ?????? ???????????? ?????? ??????????????? ????????????") {
                    val response = shouldNotThrowAny { pageObjectService.listProjectObjects(user.id, pageObject.page.project.id) }
                    response.data shouldNotContain DetailedResponse(pageObject)
                }

                Then("?????? ??????????????? ????????????") {
                    pageObject.events.forEach { event ->
                        val foundEvent = objectEventRepository.findUndeletedObjectEventById(event.id)!!

                        foundEvent shouldBe event
                        foundEvent.isDeleted shouldBe true
                    }
                }
            }
        }
    }
}