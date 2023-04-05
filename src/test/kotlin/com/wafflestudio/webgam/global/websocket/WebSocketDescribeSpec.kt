package com.wafflestudio.webgam.global.websocket

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.wafflestudio.webgam.TestUtils
import com.wafflestudio.webgam.domain.event.dto.ObjectEventDto
import com.wafflestudio.webgam.domain.event.model.TransitionType
import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import com.wafflestudio.webgam.domain.project.dto.ProjectDto
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import com.wafflestudio.webgam.global.security.model.WebgamRoles
import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.converter.GsonMessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.MimeTypeUtils.APPLICATION_JSON
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Transactional
@ActiveProfiles("test")
@Tag("Integration-Test")
@DisplayName("WebSocket 통합 테스트")
class WebSocketDescribeSpec(
        @Autowired private val jwtProvider: JwtProvider,
        @Autowired private val userRepository: UserRepository,
        @Autowired private val pageRepository: ProjectPageRepository
) : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    private inline fun <reified T> parseArray(json: String, typeToken: Type): T {
        return gson.fromJson<T>(json, typeToken)
    }

    companion object {
        private const val URL = "ws://localhost:8080/ws"
        private val messageQueue = LinkedBlockingDeque<WebSocketDto<*>>()
        private val client = WebSocketStompClient(StandardWebSocketClient())


        private val gson = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeDeserializer())
                .setExclusionStrategies(CustomExclusionStrategy())
                .create()


        private val data = TestUtils.docTestData()
        private val user = data.first()
        private val auth = WebgamAuthenticationToken(UserPrincipal(user), "")
        private val project = user.projects.first()
        private val page = project.pages.first()
        private val pageObject = page.objects.first { it.event == null }
        private val event = page.objects.first().event!!
        private val nextPage = project.pages.first()


    }

    class WebgamStompSessionHandler: StompSessionHandlerAdapter() {
        override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
            System.err.println("Received CONNECTED")
        }
    }

    class WebgamStompFrameHandler(private val queue: BlockingDeque<WebSocketDto<*>>): StompFrameHandler {
        override fun getPayloadType(headers: StompHeaders): Type {
            return WebSocketDto::class.java
        }

        override fun handleFrame(headers: StompHeaders, payload: Any?) {
            System.err.println("Payload: " + payload)
            queue.offer(payload as WebSocketDto<*>)
        }
    }

    override suspend fun beforeSpec(spec: Spec){
        client.messageConverter = (GsonMessageConverter(gson))
        userRepository.saveAll(data)
    }

    override suspend fun afterSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            userRepository.deleteAll()
        }
    }

    init {
        this.describe("Chat") {
            context("CONNECT, SUBSCRIBE, SEND chats") {
                it("Success") {

                    val (token, _) = jwtProvider.generateToken(user.userId, WebgamRoles.USER)

                    // set headers
                    val httpHeaders = WebSocketHttpHeaders()
                    httpHeaders.set("Authorization", token)
                    val headers = StompHeaders()
                    headers.set("Authorization", token)

                    // connect
                    val connection = client.connectAsync(URL, httpHeaders, headers, WebgamStompSessionHandler())
                    val session = withContext(Dispatchers.IO) {
                        connection.get(1000, TimeUnit.SECONDS)
                    }

                    // subscribe
                    session.subscribe("/topic/public", WebgamStompFrameHandler(messageQueue))

                    // send
                    val sendHeaders = StompHeaders()
                    sendHeaders.set("Authorization", token)
                    sendHeaders.destination = "/app/send"
                    sendHeaders.contentType = APPLICATION_JSON
                    val message = "Hello"
                    session.send(sendHeaders, WebSocketDto<String>(user, message))

                    withContext(Dispatchers.IO) {
                        Thread.sleep(10000) // 이거 안 넣어주면 응답 오기 전에 테스트 끝남
                    }

                    System.err.println("Checking responses...")
                    if (messageQueue.isEmpty()) System.err.println("EMPTY!!")
                    else while (messageQueue.isNotEmpty()) {
                        val response = messageQueue.poll()
                        System.err.println("response: $response")
                        response.content shouldBe message
                    }

                    session.disconnect()

                }
            }

        }

        this.describe("Project API") {
            context("Patch") {
                it("Success") {
                    val (token, _) = jwtProvider.generateToken(user.userId, WebgamRoles.USER)

                    // set headers
                    val httpHeaders = WebSocketHttpHeaders()
                    httpHeaders.set("Authorization", token)
                    val headers = StompHeaders()
                    headers.set("Authorization", token)

                    // connect
                    val connection = client.connectAsync(URL, httpHeaders, headers, WebgamStompSessionHandler())
                    val session = withContext(Dispatchers.IO) {
                        connection.get(1000, TimeUnit.SECONDS)
                    }

                    // subscribe
                    session.subscribe("/project/1", WebgamStompFrameHandler(messageQueue))

                    // send
                    val sendHeaders = StompHeaders()
                    sendHeaders.set("Authorization", token)
                    sendHeaders.destination = "/app/project/1/patch.project"
                    sendHeaders.contentType = APPLICATION_JSON
                    session.send(sendHeaders, ProjectDto.PatchRequest("title_changed"))

                    withContext(Dispatchers.IO) {
                        Thread.sleep(20000) // 이거 안 넣어주면 응답 오기 전에 테스트 끝남
                    }

                    System.err.println("Checking responses...")
                    if (messageQueue.isEmpty()) System.err.println("EMPTY!!")
                    else while (messageQueue.isNotEmpty()) {
                        val response = (messageQueue.poll())
                        System.err.println(response.content)

                        val type = object : TypeToken<ProjectDto.DetailedResponse>() {}.type
                        val responseObject = parseArray<ProjectDto.DetailedResponse>(gson.toJson(response.content), type)
                        System.err.println(responseObject)

                        responseObject.title shouldBe "title_changed"
                    }

                    session.disconnect()
                }
            }
        }

        this.describe("Page API") {

            context("Create") {
                it("Success") {
                    val (token, _) = jwtProvider.generateToken(user.userId, WebgamRoles.USER)

                    // set headers
                    val httpHeaders = WebSocketHttpHeaders()
                    httpHeaders.set("Authorization", token)
                    val headers = StompHeaders()
                    headers.set("Authorization", token)

                    // connect
                    val connection = client.connectAsync(URL, httpHeaders, headers, WebgamStompSessionHandler())
                    val session = withContext(Dispatchers.IO) {
                        connection.get(1000, TimeUnit.SECONDS)
                    }

                    // subscribe
                    session.subscribe("/project/1", WebgamStompFrameHandler(messageQueue))

                    // send
                    val sendHeaders = StompHeaders()
                    sendHeaders.set("Authorization", token)
                    sendHeaders.destination = "/app/project/1/create.page"
                    sendHeaders.contentType = APPLICATION_JSON
                    session.send(sendHeaders, ProjectPageDto.CreateRequest(projectId=1, name="new_page"))

                    withContext(Dispatchers.IO) {
                        Thread.sleep(20000) // 이거 안 넣어주면 응답 오기 전에 테스트 끝남
                    }

                    System.err.println("Checking responses...")
                    if (messageQueue.isEmpty()) System.err.println("EMPTY!!")
                    else while (messageQueue.isNotEmpty()) {
                        val response = messageQueue.poll()
                        System.err.println(response.content)

                        val type = object : TypeToken<ProjectPageDto.DetailedResponse>() {}.type
                        val responseObject = parseArray<ProjectPageDto.DetailedResponse>(gson.toJson(response.content), type)
                        System.err.println(responseObject)

                        responseObject.id shouldBe 2
                        responseObject.projectId shouldBe 1
                        responseObject.name shouldBe "new_page"
                    }

                    session.disconnect()
                }
            }

            context("Patch"){
                it("Success"){
                    val (token, _) = jwtProvider.generateToken(user.userId, WebgamRoles.USER)

                    // set headers
                    val httpHeaders = WebSocketHttpHeaders()
                    httpHeaders.set("Authorization", token)
                    val headers = StompHeaders()
                    headers.set("Authorization", token)

                    // connect
                    val connection = client.connectAsync(URL, httpHeaders, headers, WebgamStompSessionHandler())
                    val session = withContext(Dispatchers.IO) {
                        connection.get(1000, TimeUnit.SECONDS)
                    }

                    // subscribe
                    session.subscribe("/project/1", WebgamStompFrameHandler(messageQueue))

                    // send
                    val sendHeaders = StompHeaders()
                    sendHeaders.set("Authorization", token)
                    sendHeaders.destination = "/app/project/1/patch.page/1"
                    sendHeaders.contentType = APPLICATION_JSON
                    session.send(sendHeaders, ProjectPageDto.PatchRequest("changed_page_title"))

                    withContext(Dispatchers.IO) {
                        Thread.sleep(10000) // 이거 안 넣어주면 응답 오기 전에 테스트 끝남
                    }

                    System.err.println("Checking responses...")
                    if (messageQueue.isEmpty()) System.err.println("EMPTY!!")
                    else while (messageQueue.isNotEmpty()) {
                        val response = messageQueue.poll()
                        System.err.println(response.content)

                        val type = object : TypeToken<ProjectPageDto.DetailedResponse>() {}.type
                        val responseObject = parseArray<ProjectPageDto.DetailedResponse>(gson.toJson(response.content), type)
                        System.err.println(responseObject)

                        responseObject.name shouldBe "changed_page_title"
                    }

                    session.disconnect()
                }
            }

            context("Delete"){
                it("Success"){
                    val (token, _) = jwtProvider.generateToken(user.userId, WebgamRoles.USER)

                    // set headers
                    val httpHeaders = WebSocketHttpHeaders()
                    httpHeaders.set("Authorization", token)
                    val headers = StompHeaders()
                    headers.set("Authorization", token)

                    // connect
                    val connection = client.connectAsync(URL, httpHeaders, headers, WebgamStompSessionHandler())
                    val session = withContext(Dispatchers.IO) {
                        connection.get(1000, TimeUnit.SECONDS)
                    }

                    // subscribe
                    session.subscribe("/project/1", WebgamStompFrameHandler(messageQueue))

                    // send
                    val sendHeaders = StompHeaders()
                    sendHeaders.set("Authorization", token)
                    sendHeaders.destination = "/app/project/1/delete.page/2"
                    sendHeaders.contentType = APPLICATION_JSON
                    session.send(sendHeaders, Any())

                    withContext(Dispatchers.IO) {
                        Thread.sleep(10000) // 이거 안 넣어주면 응답 오기 전에 테스트 끝남
                    }

                    System.err.println("Checking responses...")
                    if (messageQueue.isEmpty()) System.err.println("EMPTY!!")
                    else while (messageQueue.isNotEmpty()) {
                        System.err.println("response: ${messageQueue.poll()}")
                    }

                    session.disconnect()
                }
            }
        }

        this.describe("Object API"){
            context("Create"){
                it("Success"){
                    val (token, _) = jwtProvider.generateToken(user.userId, WebgamRoles.USER)

                    // set headers
                    val httpHeaders = WebSocketHttpHeaders()
                    httpHeaders.set("Authorization", token)
                    val headers = StompHeaders()
                    headers.set("Authorization", token)

                    // connect
                    val connection = client.connectAsync(URL, httpHeaders, headers, WebgamStompSessionHandler())
                    val session = withContext(Dispatchers.IO) {
                        connection.get(1000, TimeUnit.SECONDS)
                    }

                    // subscribe
                    session.subscribe("/project/1", WebgamStompFrameHandler(messageQueue))

                    // send
                    val sendHeaders = StompHeaders()
                    sendHeaders.set("Authorization", token)
                    sendHeaders.destination = "/app/project/1/create.object"
                    sendHeaders.contentType = APPLICATION_JSON

                    session.send(sendHeaders,
                            PageObjectDto.CreateRequest(1, "new_object", PageObjectType.DEFAULT,
                                            30, 30, 2, 2, 0, 1,
                                            "hi", null, 2, 2, null, null, null, null,
                                             false, 180))


                    withContext(Dispatchers.IO) {
                        Thread.sleep(10000) // 이거 안 넣어주면 응답 오기 전에 테스트 끝남
                    }

                    System.err.println("Checking responses...")
                    if (messageQueue.isEmpty()) System.err.println("EMPTY!!")
                    else while (messageQueue.isNotEmpty()) {
                        val response = messageQueue.poll()
                        System.err.println(response.content)

                        val type = object : TypeToken<PageObjectDto.SimpleResponse>() {}.type
                        val responseObject = parseArray<PageObjectDto.SimpleResponse>(gson.toJson(response.content), type)
                        System.err.println(responseObject)

                        responseObject.id shouldBe 3
                        responseObject.name shouldBe "new_object"
                        responseObject.type shouldBe PageObjectType.DEFAULT
                        responseObject.width shouldBe 30
                        responseObject.height shouldBe 30
                        responseObject.xPosition shouldBe 2
                        responseObject.yPosition shouldBe 2
                        responseObject.zIndex shouldBe 0
                        responseObject.opacity shouldBe 1
                        responseObject.textContent shouldBe "hi"
                        responseObject.fontSize shouldBe null
                        responseObject.lineHeight shouldBe 2
                        responseObject.letterSpacing shouldBe 2
                        responseObject.backgroundColor shouldBe null
                        responseObject.strokeWidth shouldBe null
                        responseObject.strokeColor shouldBe null
                        responseObject.imageSource shouldBe null
                        responseObject.isReversed shouldBe false
                        responseObject.rotateDegree shouldBe 180

                    }

                    session.disconnect()
                }
            }

            context("Patch"){
                it("Success") {
                    val (token, _) = jwtProvider.generateToken(user.userId, WebgamRoles.USER)

                    // set headers
                    val httpHeaders = WebSocketHttpHeaders()
                    httpHeaders.set("Authorization", token)
                    val headers = StompHeaders()
                    headers.set("Authorization", token)

                    // connect
                    val connection = client.connectAsync(URL, httpHeaders, headers, WebgamStompSessionHandler())
                    val session = withContext(Dispatchers.IO) {
                        connection.get(1000, TimeUnit.SECONDS)
                    }

                    // subscribe
                    session.subscribe("/project/1", WebgamStompFrameHandler(messageQueue))

                    // send
                    val sendHeaders = StompHeaders()
                    sendHeaders.set("Authorization", token)
                    sendHeaders.destination = "/app/project/1/patch.object/1"
                    sendHeaders.contentType = APPLICATION_JSON
                    session.send(sendHeaders, PageObjectDto.PatchRequest(PageObjectType.DEFAULT, 20, 20,
                            3, 3, 0, 3,"changed_text", 5, 16,
                            2, "#FFFFFF", 8, "#000000",
                            "", true,90))

                    withContext(Dispatchers.IO) {
                        Thread.sleep(10000) // 이거 안 넣어주면 응답 오기 전에 테스트 끝남
                    }

                    System.err.println("Checking responses...")
                    if (messageQueue.isEmpty()) System.err.println("EMPTY!!")
                    else while (messageQueue.isNotEmpty()) {
                        val response = messageQueue.poll()
                        System.err.println(response.content)

                        val type = object : TypeToken<PageObjectDto.DetailedResponse>() {}.type
                        val responseObject = parseArray<PageObjectDto.DetailedResponse>(gson.toJson(response.content), type)
                        System.err.println(responseObject)

                        responseObject.type shouldBe PageObjectType.DEFAULT
                        responseObject.width shouldBe 20
                        responseObject.height shouldBe 20
                        responseObject.xPosition shouldBe 3
                        responseObject.yPosition shouldBe 3
                        responseObject.zIndex shouldBe 0
                        responseObject.opacity shouldBe 3
                        responseObject.textContent shouldBe "changed_text"
                        responseObject.fontSize shouldBe 5
                        responseObject.lineHeight shouldBe 16
                        responseObject.letterSpacing shouldBe 2
                        responseObject.backgroundColor shouldBe "#FFFFFF"
                        responseObject.strokeWidth shouldBe 8
                        responseObject.strokeColor shouldBe "#000000"
                        responseObject.imageSource shouldBe ""
                        responseObject.isReversed shouldBe true
                        responseObject.rotateDegree shouldBe 90


                    }

                    session.disconnect()
                }
            }

            context("Delete"){
                it("Success") {
                    val (token, _) = jwtProvider.generateToken(user.userId, WebgamRoles.USER)

                    // set headers
                    val httpHeaders = WebSocketHttpHeaders()
                    httpHeaders.set("Authorization", token)
                    val headers = StompHeaders()
                    headers.set("Authorization", token)

                    // connect
                    val connection = client.connectAsync(URL, httpHeaders, headers, WebgamStompSessionHandler())
                    val session = withContext(Dispatchers.IO) {
                        connection.get(1000, TimeUnit.SECONDS)
                    }

                    // subscribe
                    session.subscribe("/project/1", WebgamStompFrameHandler(messageQueue))

                    // send
                    val sendHeaders = StompHeaders()
                    sendHeaders.set("Authorization", token)
                    sendHeaders.destination = "/app/project/1/delete.object/3"
                    sendHeaders.contentType = APPLICATION_JSON
                    session.send(sendHeaders, Any())

                    withContext(Dispatchers.IO) {
                        Thread.sleep(10000) // 이거 안 넣어주면 응답 오기 전에 테스트 끝남
                    }

                    System.err.println("Checking responses...")
                    if (messageQueue.isEmpty()) System.err.println("EMPTY!!")
                    else while (messageQueue.isNotEmpty()) {
                        val response = messageQueue.poll()
                        System.err.println(response.content)
                    }

                    session.disconnect()
                }
            }
        }

        this.describe("Event API"){
            context("Create"){
                it("Success"){
                    val (token, _) = jwtProvider.generateToken(user.userId, WebgamRoles.USER)

                    // set headers
                    val httpHeaders = WebSocketHttpHeaders()
                    httpHeaders.set("Authorization", token)
                    val headers = StompHeaders()
                    headers.set("Authorization", token)

                    // connect
                    val connection = client.connectAsync(URL, httpHeaders, headers, WebgamStompSessionHandler())
                    val session = withContext(Dispatchers.IO) {
                        connection.get(1000, TimeUnit.SECONDS)
                    }

                    // subscribe
                    session.subscribe("/project/1", WebgamStompFrameHandler(messageQueue))

                    // send
                    val sendHeaders = StompHeaders()
                    sendHeaders.set("Authorization", token)
                    sendHeaders.destination = "/app/project/1/create.event"
                    sendHeaders.contentType = APPLICATION_JSON
                    session.send(sendHeaders, ObjectEventDto.CreateRequest(2, TransitionType.DEFAULT, 1))


                    withContext(Dispatchers.IO) {
                        Thread.sleep(10000) // 이거 안 넣어주면 응답 오기 전에 테스트 끝남
                    }

                    System.err.println("Checking responses...")
                    if (messageQueue.isEmpty()) System.err.println("EMPTY!!")
                    else while (messageQueue.isNotEmpty()) {
                        val response = messageQueue.poll()
                        System.err.println(response.content)

                        val type = object : TypeToken<ObjectEventDto.SimpleResponse>() {}.type
                        val responseObject = parseArray<ObjectEventDto.SimpleResponse>(gson.toJson(response.content), type)
                        System.err.println(responseObject)

                        responseObject.id shouldBe 2
                        responseObject.transitionType shouldBe TransitionType.DEFAULT
                        responseObject.nextPage!!.id shouldBe 1
                    }

                    session.disconnect()
                }
            }

            context("Patch"){
                it("Success"){
                    val (token, _) = jwtProvider.generateToken(user.userId, WebgamRoles.USER)

                    // set headers
                    val httpHeaders = WebSocketHttpHeaders()
                    httpHeaders.set("Authorization", token)
                    val headers = StompHeaders()
                    headers.set("Authorization", token)

                    // connect
                    val connection = client.connectAsync(URL, httpHeaders, headers, WebgamStompSessionHandler())
                    val session = withContext(Dispatchers.IO) {
                        connection.get(1000, TimeUnit.SECONDS)
                    }

                    // subscribe
                    session.subscribe("/project/1", WebgamStompFrameHandler(messageQueue))

                    // send
                    val sendHeaders = StompHeaders()
                    sendHeaders.set("Authorization", token)
                    sendHeaders.destination = "/app/project/1/patch.event/1"
                    sendHeaders.contentType = APPLICATION_JSON
                    session.send(sendHeaders, ObjectEventDto.PatchRequest(TransitionType.DEFAULT, null))

                    withContext(Dispatchers.IO) {
                        Thread.sleep(10000) // 이거 안 넣어주면 응답 오기 전에 테스트 끝남
                    }

                    System.err.println("Checking responses...")
                    if (messageQueue.isEmpty()) System.err.println("EMPTY!!")
                    else while (messageQueue.isNotEmpty()) {
                        val response = messageQueue.poll()
                        System.err.println(response.content)

                        val type = object : TypeToken<ObjectEventDto.SimpleResponse>() {}.type
                        val responseObject = parseArray<ObjectEventDto.SimpleResponse>(gson.toJson(response.content), type)
                        System.err.println(responseObject)

                        responseObject.transitionType shouldBe TransitionType.DEFAULT
                        responseObject.nextPage!!.id shouldBe 1

                    }

                    session.disconnect()


                }
            }

            context("Delete"){
                it("Success"){
                    val (token, _) = jwtProvider.generateToken(user.userId, WebgamRoles.USER)

                    // set headers
                    val httpHeaders = WebSocketHttpHeaders()
                    httpHeaders.set("Authorization", token)
                    val headers = StompHeaders()
                    headers.set("Authorization", token)

                    // connect
                    val connection = client.connectAsync(URL, httpHeaders, headers, WebgamStompSessionHandler())
                    val session = withContext(Dispatchers.IO) {
                        connection.get(1000, TimeUnit.SECONDS)
                    }

                    // subscribe
                    session.subscribe("/project/1", WebgamStompFrameHandler(messageQueue))

                    // send
                    val sendHeaders = StompHeaders()
                    sendHeaders.set("Authorization", token)
                    sendHeaders.destination = "/app/project/1/delete.event/1"
                    sendHeaders.contentType = APPLICATION_JSON
                    session.send(sendHeaders, Any())

                    withContext(Dispatchers.IO) {
                        Thread.sleep(10000) // 이거 안 넣어주면 응답 오기 전에 테스트 끝남
                    }

                    System.err.println("Checking responses...")
                    if (messageQueue.isEmpty()) System.err.println("EMPTY!!")
                    else while (messageQueue.isNotEmpty()) {
                        val response = messageQueue.poll()
                        System.err.println(response.content)
                    }

                    session.disconnect()
                }
            }
        }

    }
}