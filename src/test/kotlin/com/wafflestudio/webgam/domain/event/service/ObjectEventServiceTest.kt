package com.wafflestudio.webgam.domain.event.service

import com.wafflestudio.webgam.domain.event.dto.ObjectEventDto.*
import com.wafflestudio.webgam.domain.event.exception.MultipleEventAllocationException
import com.wafflestudio.webgam.domain.event.exception.NonAccessibleObjectEventException
import com.wafflestudio.webgam.domain.event.exception.ObjectEventNotFoundException
import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import com.wafflestudio.webgam.domain.event.model.TransitionType.DEFAULT
import com.wafflestudio.webgam.domain.event.repository.ObjectEventRepository
import com.wafflestudio.webgam.domain.`object`.exception.NonAccessiblePageObjectException
import com.wafflestudio.webgam.domain.`object`.exception.PageObjectNotFoundException
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType.TEXT
import com.wafflestudio.webgam.domain.`object`.repository.PageObjectRepository
import com.wafflestudio.webgam.domain.page.exception.NonAccessibleProjectPageException
import com.wafflestudio.webgam.domain.page.exception.ProjectPageNotFoundException
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Tag

@Tag("Unit-Test")
@DisplayName("ObjectEventService 단위 테스트")
class ObjectEventServiceTest: DescribeSpec() {

    companion object {
        private val projectPageRepository = mockk<ProjectPageRepository>()
        private val pageObjectRepository = mockk<PageObjectRepository>()
        private val objectEventRepository = mockk<ObjectEventRepository>()
        private val objectEventService = ObjectEventService(projectPageRepository, pageObjectRepository, objectEventRepository)
        private val page = mockk<ProjectPage>()
        private val nonAccessiblePage = mockk<ProjectPage>()
        private val pageObject = PageObject(page, "test-object", TEXT, 0, 0, 0, 0, 0, "", 0, "", null)
        private val nonAccessibleObject = mockk<PageObject>()
        private val objectWithEvent = mockk<PageObject>()
        private val event = ObjectEvent(pageObject, null, DEFAULT)
        private val nonAccessibleEvent = mockk<ObjectEvent>()
        private val dummyEvent = mockk<ObjectEvent>()
        private val triggeredEvents = mutableListOf<ObjectEvent>()
        private val createdEventSlot = CapturingSlot<ObjectEvent>()

        private const val USER_ID = 1L
        private const val OBJ_ID = 1L
        private const val NON_ACC_OBJ_ID = 2L
        private const val NON_EXI_OBJ_ID = 3L
        private const val EVENT_EXI_OBJ_ID = 4L
        private const val PAGE_ID = 1L
        private const val NON_ACC_PAGE_ID = 2L
        private const val NON_EXI_PAGE_ID = 3L
        private const val EVENT_ID = 1L
        private const val NON_ACC_EVENT_ID = 2L
        private const val NON_EXI_EVENT_ID = 3L
    }

    override suspend fun beforeSpec(spec: Spec) {
        every { projectPageRepository.findUndeletedProjectPageById(PAGE_ID) } returns page
        every { projectPageRepository.findUndeletedProjectPageById(NON_ACC_PAGE_ID) } returns nonAccessiblePage
        every { projectPageRepository.findUndeletedProjectPageById(NON_EXI_PAGE_ID) } returns null

        every { pageObjectRepository.findUndeletedPageObjectById(OBJ_ID) } returns pageObject
        every { pageObjectRepository.findUndeletedPageObjectById(NON_ACC_OBJ_ID) } returns nonAccessibleObject
        every { pageObjectRepository.findUndeletedPageObjectById(NON_EXI_OBJ_ID) } returns null
        every { pageObjectRepository.findUndeletedPageObjectById(EVENT_EXI_OBJ_ID) } returns objectWithEvent

        every { objectEventRepository.findUndeletedObjectEventById(EVENT_ID) } returns event
        every { objectEventRepository.findUndeletedObjectEventById(NON_ACC_EVENT_ID) } returns nonAccessibleEvent
        every { objectEventRepository.findUndeletedObjectEventById(NON_EXI_EVENT_ID) } returns null

        every { objectEventRepository.save(capture(createdEventSlot)) } answers { firstArg() }

        every { nonAccessiblePage.isAccessibleTo(USER_ID) } returns false
        every { nonAccessibleObject.isAccessibleTo(USER_ID) } returns false
        every { nonAccessibleEvent.isAccessibleTo(USER_ID) } returns false

        every { objectWithEvent.event } returns dummyEvent
        every { objectWithEvent.id } returns EVENT_EXI_OBJ_ID

        every { page.triggeredEvents } returns triggeredEvents

        every { page.isAccessibleTo(USER_ID) } returns true
        every { objectWithEvent.isAccessibleTo(USER_ID) } returns true
        every { event.isAccessibleTo(USER_ID) } returns true
    }

    override suspend fun beforeContainer(testCase: TestCase) {
        pageObject.event?. let { pageObject.event = null }
        page.triggeredEvents.clear()
    }

    init {
        this.describe("getEvent 호출될 때") {
            context("성공적인 경우") {
                val response = withContext(Dispatchers.IO) { objectEventService.getEvent(USER_ID, EVENT_ID) }
                it("ObjectEvent가 Simple Response DTO로 반환된다") {
                    response shouldBe SimpleResponse(event)
                }
            }

            context("해당 ID를 갖는 이벤트가 존재하지 않거나 삭제됐으면") {
                it("ObjectEventNotFoundException 예외를 던진다") {
                    shouldThrow<ObjectEventNotFoundException> { objectEventService.getEvent(USER_ID, NON_EXI_EVENT_ID) }
                }
            }

            context("해당 ID를 갖는 PageObject에 접근할 수 없으면") {
                it("NonAccessibleObjectEventException 예외를 던진다") {
                    shouldThrow<NonAccessibleObjectEventException> { objectEventService.getEvent(USER_ID, NON_ACC_EVENT_ID) }
                }
            }
        }

        this.describe("createEvent 호출될 때") {
            context("성공적인 경우") {
                val responseGivenNextPageId = withContext(Dispatchers.IO) {
                    objectEventService.createEvent(USER_ID, CreateRequest(OBJ_ID, DEFAULT, PAGE_ID))
                }
                val createdEvent = createdEventSlot.captured

                it("생성된 ObjectEvent가 SimpleResponse DTO로 반환된다") {
                    responseGivenNextPageId shouldBe SimpleResponse(createdEvent)
                }

                it("새로 생성된 이벤트는 연결된 오브젝트의 event로 설정된다") {
                    pageObject.event shouldBe createdEvent
                }

                it("다음 페이지가 있으면, 페이지 triggeredEvents에 추가된다") {
                    page.triggeredEvents shouldHaveSingleElement createdEvent
                }
            }

            context("objectId의 오브젝트가 존재하지 않거나 삭제됐으면") {
                it("PageObjectNotFoundException 예외를 던진다") {
                    shouldThrow<PageObjectNotFoundException> {
                        objectEventService.createEvent(USER_ID, CreateRequest(NON_EXI_OBJ_ID, DEFAULT, null))
                    }
                }
            }

            context("objectId의 오브젝트에 접근할 수 없으면") {
                it("NonAccessiblePageObjectException 예외를 던진다") {
                    shouldThrow<NonAccessiblePageObjectException> {
                        objectEventService.createEvent(USER_ID, CreateRequest(NON_ACC_OBJ_ID, DEFAULT, null))
                    }
                }
            }

            context("objectId의 오브젝트에 이미 이벤트가 설정되어 있으면") {
                it("MultipleEventAllocationException 예외를 던진다") {
                    shouldThrow<MultipleEventAllocationException> {
                        objectEventService.createEvent(USER_ID, CreateRequest(EVENT_EXI_OBJ_ID, DEFAULT, null))
                    }
                }
            }

            context("nextPageId의 페이지가 존재하지 않거나 삭제됐으면") {
                it("ProjectPageNotFoundException 예외를 던진다") {
                    shouldThrow<ProjectPageNotFoundException> {
                        objectEventService.createEvent(USER_ID, CreateRequest(OBJ_ID, DEFAULT, NON_EXI_PAGE_ID))
                    }
                }
            }

            context("nextPageId의 페이지에 접근할 수 없으면") {
                it("NonAccessibleProjectPageException 예외를 던진다") {
                    shouldThrow<NonAccessibleProjectPageException> {
                        objectEventService.createEvent(USER_ID, CreateRequest(OBJ_ID, DEFAULT, NON_ACC_PAGE_ID))
                    }
                }
            }
        }

        this.describe("updateEvent 호출될 때") {
            context("성공적인 경우: transitionType이 NULL이 아닐 때") {
                val response = withContext(Dispatchers.IO) {
                    objectEventService.updateEvent(USER_ID, EVENT_ID, PatchRequest(null, PAGE_ID))
                }

                it("수정된 ObjectEvent가 Simple Response DTO로 반환된다") {
                    response shouldBe SimpleResponse(event)
                }

                it("페이지 triggeredEvents에 추가된다") {
                    page.triggeredEvents shouldContain event
                }

                it("request의 NULL인 필드는 수정되지 않는다") {
                    event.transitionType shouldBe DEFAULT
                }
            }

            context("해당 ID를 갖는 이벤트가 존재하지 않거나 삭제됐으면") {
                it("ObjectEventNotFoundException 예외를 던진다") {
                    shouldThrow<ObjectEventNotFoundException> {
                        objectEventService.updateEvent(USER_ID, NON_EXI_EVENT_ID, PatchRequest(null, null))
                    }
                }
            }

            context("해당 ID를 갖는 PageObject에 접근할 수 없으면") {
                it("NonAccessibleObjectEventException 예외를 던진다") {
                    shouldThrow<NonAccessibleObjectEventException> {
                        objectEventService.updateEvent(USER_ID, NON_ACC_EVENT_ID, PatchRequest(null, null))
                    }
                }
            }
        }

        this.describe("deleteEvent 호출될 때") {
            context("성공적인 경우") {
                it("반환 값이 없으며, 예외도 던지지 않는다") {
                    shouldNotThrowAny { objectEventService.deleteEvent(USER_ID, EVENT_ID) }
                }

                it("해당 ID를 갖는 이벤트의 isDeleted는 true가 된다") {
                    event.isDeleted shouldBe true
                }

                it("연결된 오브젝트의 event는 null이 된다") {
                    pageObject.event shouldBe null
                }

                it("해당 ID를 갖는 이벤트가 연결된 오브젝트의 deletedEvents로 추가된다") {
                    pageObject.deletedEvents shouldContain event
                }
            }

            context("해당 ID를 갖는 이벤트가 존재하지 않거나 삭제됐으면") {
                it("ObjectEventNotFoundException 예외를 던진다") {
                    shouldThrow<ObjectEventNotFoundException> { objectEventService.deleteEvent(USER_ID, NON_EXI_EVENT_ID) }
                }
            }

            context("해당 ID를 갖는 PageObject에 접근할 수 없으면") {
                it("NonAccessibleObjectEventException 예외를 던진다") {
                    shouldThrow<NonAccessibleObjectEventException> { objectEventService.deleteEvent(USER_ID, NON_ACC_EVENT_ID) }
                }
            }
        }

    }
}