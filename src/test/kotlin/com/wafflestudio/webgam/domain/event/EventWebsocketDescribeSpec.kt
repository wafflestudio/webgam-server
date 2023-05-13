package com.wafflestudio.webgam.domain.event

import com.google.gson.reflect.TypeToken
import com.wafflestudio.webgam.domain.event.dto.ObjectEventDto
import com.wafflestudio.webgam.domain.event.model.TransitionType
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import com.wafflestudio.webgam.global.security.model.WebgamRoles
import com.wafflestudio.webgam.global.websocket.WebSocketDescribeSpec
import io.kotest.core.annotation.DisplayName
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.MimeTypeUtils
import org.springframework.web.socket.WebSocketHttpHeaders
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Transactional
@ActiveProfiles("test")
@Tag("Integration-Test")
@DisplayName("Event Websocket 통합 테스트")
class EventWebsocketDescribeSpec(
        @Autowired private val jwtProvider: JwtProvider,
        @Autowired private val userRepository: UserRepository
): WebSocketDescribeSpec(userRepository) {

    private inline fun <reified T> parseArray(json: String, typeToken: Type): T {
        return gson.fromJson<T>(json, typeToken)
    }

    init{
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
                    sendHeaders.contentType = MimeTypeUtils.APPLICATION_JSON
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
                    sendHeaders.contentType = MimeTypeUtils.APPLICATION_JSON
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
                    sendHeaders.contentType = MimeTypeUtils.APPLICATION_JSON
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