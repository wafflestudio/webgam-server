package com.wafflestudio.webgam.domain.`object`

import com.google.gson.reflect.TypeToken
import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType
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
@DisplayName("Object Websocket 통합 테스트")
class ObjectWebsocketDescribeSpec(
        @Autowired private val jwtProvider: JwtProvider,
        @Autowired private val userRepository: UserRepository
): WebSocketDescribeSpec(userRepository) {

    private inline fun <reified T> parseArray(json: String, typeToken: Type): T {
        return gson.fromJson<T>(json, typeToken)
    }

    init{
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
                    sendHeaders.contentType = MimeTypeUtils.APPLICATION_JSON

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
                    sendHeaders.contentType = MimeTypeUtils.APPLICATION_JSON
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