package com.wafflestudio.webgam.domain.event.service

import com.wafflestudio.webgam.domain.event.dto.ObjectEventDto.*
import com.wafflestudio.webgam.domain.event.exception.LinkNonRelatedPageException
import com.wafflestudio.webgam.domain.event.exception.MultipleEventAllocationException
import com.wafflestudio.webgam.domain.event.exception.NonAccessibleObjectEventException
import com.wafflestudio.webgam.domain.event.exception.ObjectEventNotFoundException
import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import com.wafflestudio.webgam.domain.event.model.TransitionType.DEFAULT
import com.wafflestudio.webgam.domain.event.repository.ObjectEventRepository
import com.wafflestudio.webgam.domain.`object`.exception.NonAccessiblePageObjectException
import com.wafflestudio.webgam.domain.`object`.exception.PageObjectNotFoundException
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.`object`.repository.PageObjectRepository
import com.wafflestudio.webgam.domain.page.exception.NonAccessibleProjectPageException
import com.wafflestudio.webgam.domain.page.exception.ProjectPageNotFoundException
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import com.wafflestudio.webgam.domain.project.model.Project
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

@DisplayName("ObjectEventService 단위 테스트")
class ObjectEventServiceTest: DescribeSpec() {

    companion object {
        private val projectPageRepository = mockk<ProjectPageRepository>()
        private val pageObjectRepository = mockk<PageObjectRepository>()
        private val objectEventRepository = mockk<ObjectEventRepository>()
        private val objectEventService = ObjectEventService(projectPageRepository, pageObjectRepository, objectEventRepository)
        private val project = mockk<Project>()
        private val otherProject = mockk<Project>()
        private val page = mockk<ProjectPage>()
        private val nonAccessiblePage = mockk<ProjectPage>()
        private val pageInOtherProject = mockk<ProjectPage>()
        private val pageObject = mockk<PageObject>()
        private val nonAccessibleObject = mockk<PageObject>()
        private val objectWithEvent = mockk<PageObject>()
        private val event = ObjectEvent(pageObject, null, DEFAULT)
        private val nonAccessibleEvent = mockk<ObjectEvent>()
        private val dummyEvent = mockk<ObjectEvent>()

        private const val USER_ID = 1L
        private const val NORMAL = 1L
        private const val DELETED = 2L
        private const val NON_ACCESSIBLE = 3L
        private const val PRE_ALLOCATED = 4L
        private const val OTHER = 4L
    }

    override suspend fun beforeSpec(spec: Spec) {
        every { projectPageRepository.findUndeletedProjectPageById(NORMAL) } returns page
        every { projectPageRepository.findUndeletedProjectPageById(DELETED) } returns null
        every { projectPageRepository.findUndeletedProjectPageById(NON_ACCESSIBLE) } returns nonAccessiblePage
        every { projectPageRepository.findUndeletedProjectPageById(OTHER) } returns pageInOtherProject

        every { pageObjectRepository.findUndeletedPageObjectById(NORMAL) } returns pageObject
        every { pageObjectRepository.findUndeletedPageObjectById(DELETED) } returns null
        every { pageObjectRepository.findUndeletedPageObjectById(NON_ACCESSIBLE) } returns nonAccessibleObject
        every { pageObjectRepository.findUndeletedPageObjectById(PRE_ALLOCATED) } returns objectWithEvent

        every { objectEventRepository.save(any()) } returns event
        every { objectEventRepository.findUndeletedObjectEventById(NORMAL) } returns event
        every { objectEventRepository.findUndeletedObjectEventById(DELETED) } returns null
        every { objectEventRepository.findUndeletedObjectEventById(NON_ACCESSIBLE) } returns nonAccessibleEvent

        every { nonAccessiblePage.isAccessibleTo(USER_ID) } returns false
        every { nonAccessibleObject.isAccessibleTo(USER_ID) } returns false
        every { nonAccessibleEvent.isAccessibleTo(USER_ID) } returns false

        every { page.isAccessibleTo(USER_ID) } returns true
        every { page.triggeredEvents } returns mutableListOf()
        every { page.project } returns project
        every { page.id } returns NORMAL
        every { page.name } returns "page"
        every { page.createdAt } returns LocalDateTime.now()
        every { page.modifiedAt } returns LocalDateTime.now()
        every { page.createdBy } returns ""
        every { page.modifiedBy } returns ""

        every { pageInOtherProject.isAccessibleTo(USER_ID) } returns true
        every { pageInOtherProject.project } returns otherProject

        every { pageObject.isAccessibleTo(USER_ID) } returns true
        every { pageObject.page } returns page
        every { pageObject.event } returns null
        every { pageObject.events } returns mutableListOf()

        every { objectWithEvent.isAccessibleTo(USER_ID) } returns true
        every { objectWithEvent.event } returns dummyEvent
    }

    init {
        this.describe("getEvent 호출될 때") {
            context("성공적인 경우") {
                it("ObjectEvent가 Simple Response DTO로 반환된다") {
                    val response = shouldNotThrowAny { objectEventService.getEvent(USER_ID, NORMAL) }
                    response shouldBe SimpleResponse(event)
                }
            }

            context("해당 ID를 갖는 이벤트가 존재하지 않거나 삭제됐으면") {
                it("ObjectEventNotFoundException 예외를 던진다") {
                    shouldThrow<ObjectEventNotFoundException> { objectEventService.getEvent(USER_ID, DELETED) }
                }
            }

            context("해당 ID를 갖는 PageObject에 접근할 수 없으면") {
                it("NonAccessibleObjectEventException 예외를 던진다") {
                    shouldThrow<NonAccessibleObjectEventException> { objectEventService.getEvent(USER_ID, NON_ACCESSIBLE) }
                }
            }
        }

        this.describe("createEvent 호출될 때") {
            context("성공적인 경우") {
                val request = CreateRequest(NORMAL, DEFAULT, NORMAL)

                it("생성된 ObjectEvent가 SimpleResponse DTO로 반환된다") {
                    val response = shouldNotThrowAny { objectEventService.createEvent(USER_ID, request) }
                    response shouldBe SimpleResponse(event)
                }
            }

            context("objectId의 오브젝트가 존재하지 않거나 삭제됐으면") {
                val request = CreateRequest(DELETED, DEFAULT, null)

                it("PageObjectNotFoundException 예외를 던진다") {
                    shouldThrow<PageObjectNotFoundException> { objectEventService.createEvent(USER_ID, request) }
                }
            }

            context("objectId의 오브젝트에 접근할 수 없으면") {
                val request = CreateRequest(NON_ACCESSIBLE, DEFAULT, null)

                it("NonAccessiblePageObjectException 예외를 던진다") {
                    shouldThrow<NonAccessiblePageObjectException> { objectEventService.createEvent(USER_ID, request) }
                }
            }

            context("objectId의 오브젝트에 이미 이벤트가 설정되어 있으면") {
                val request = CreateRequest(PRE_ALLOCATED, DEFAULT, null)

                it("MultipleEventAllocationException 예외를 던진다") {
                    shouldThrow<MultipleEventAllocationException> { objectEventService.createEvent(USER_ID, request) }
                }
            }

            context("nextPageId의 페이지가 존재하지 않거나 삭제됐으면") {
                val request = CreateRequest(NORMAL, DEFAULT, DELETED)

                it("ProjectPageNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectPageNotFoundException> {
                        objectEventService.createEvent(USER_ID, request)
                    }
                }
            }

            context("nextPageId의 페이지에 접근할 수 없으면") {
                val request = CreateRequest(NORMAL, DEFAULT, NON_ACCESSIBLE)

                it("NonAccessibleProjectPageException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectPageException> { objectEventService.createEvent(USER_ID, request) }
                }
            }

            context("nextPageId의 페이지가 다른 프로젝트에 있는 경우") {
                val request = CreateRequest(NORMAL, DEFAULT, OTHER)

                it("LinkNonRelatedPageException 예외를 던진다") {
                    shouldThrow<LinkNonRelatedPageException> { objectEventService.createEvent(USER_ID, request) }
                }
            }
        }

        this.describe("updateEvent 호출될 때") {
            context("성공적인 경우") {
                val request = PatchRequest(null, NORMAL)

                it("수정된 ObjectEvent가 Simple Response DTO로 반환된다") {
                    val response = shouldNotThrowAny { objectEventService.updateEvent(USER_ID, NORMAL, request) }
                    response shouldBe SimpleResponse(event)
                }
            }

            context("해당 ID를 갖는 이벤트가 존재하지 않거나 삭제됐으면") {
                val request = PatchRequest(null, NORMAL)

                it("ObjectEventNotFoundException 예외를 던진다") {
                    shouldThrow<ObjectEventNotFoundException> {
                        objectEventService.updateEvent(USER_ID, DELETED, request)
                    }
                }
            }

            context("해당 ID를 갖는 PageObject에 접근할 수 없으면") {
                val request = PatchRequest(null, NORMAL)

                it("NonAccessibleObjectEventException 예외를 던진다") {
                    shouldThrow<NonAccessibleObjectEventException> {
                        objectEventService.updateEvent(USER_ID, NON_ACCESSIBLE, request)
                    }
                }
            }

            context("nextPageId의 페이지가 존재하지 않거나 삭제됐으면") {
                val request = PatchRequest(null, DELETED)

                it("ProjectPageNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectPageNotFoundException> {
                        objectEventService.updateEvent(USER_ID, NORMAL, request)
                    }
                }
            }

            context("nextPageId의 페이지에 접근할 수 없으면") {
                val request = PatchRequest(null, NON_ACCESSIBLE)

                it("NonAccessibleProjectPageException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectPageException> {
                        objectEventService.updateEvent(USER_ID, NORMAL, request)
                    }
                }
            }

            context("nextPageId의 페이지가 다른 프로젝트에 있는 경우") {
                val request = PatchRequest(null, OTHER)

                it("LinkNonRelatedPageException 예외를 던진다") {
                    shouldThrow<LinkNonRelatedPageException> { objectEventService.updateEvent(USER_ID, NORMAL, request) }
                }
            }
        }

        this.describe("deleteEvent 호출될 때") {
            context("성공적인 경우") {
                it("반환 값이 없으며, 예외도 던지지 않는다") {
                    shouldNotThrowAny { objectEventService.deleteEvent(USER_ID, NORMAL) }
                }
            }

            context("해당 ID를 갖는 이벤트가 존재하지 않거나 삭제됐으면") {
                it("ObjectEventNotFoundException 예외를 던진다") {
                    shouldThrow<ObjectEventNotFoundException> { objectEventService.deleteEvent(USER_ID, DELETED) }
                }
            }

            context("해당 ID를 갖는 PageObject에 접근할 수 없으면") {
                it("NonAccessibleObjectEventException 예외를 던진다") {
                    shouldThrow<NonAccessibleObjectEventException> { objectEventService.deleteEvent(USER_ID, NON_ACCESSIBLE) }
                }
            }
        }

    }
}