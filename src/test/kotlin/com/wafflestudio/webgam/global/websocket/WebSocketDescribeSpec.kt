package com.wafflestudio.webgam.global.websocket

import com.wafflestudio.webgam.global.security.dto.AuthDto
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import com.wafflestudio.webgam.global.security.model.WebgamRoles
import com.wafflestudio.webgam.global.security.service.AuthService
import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.converter.GsonMessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.MimeTypeUtils.APPLICATION_JSON
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.lang.reflect.Type
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Tag("Integration-Test")
@DisplayName("WebSocket 통합 테스트")
class WebSocketDescribeSpec(
        @Autowired private val jwtProvider: JwtProvider,
        @Autowired private val authService: AuthService
) : DescribeSpec() {
    companion object {
        private const val URL = "ws://localhost:8080/ws"
        private val messageQueue = LinkedBlockingDeque<WebSocketDto.ChatMessage>()
    }

    private val logger: Logger = LoggerFactory.getLogger(WebSocketDescribeSpec::class.java)

    class WebgamStompSessionHandler: StompSessionHandlerAdapter() {
        override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
            System.err.println("Received CONNECTED")
        }
    }

    class WebgamStompFrameHandler(private val queue: BlockingDeque<WebSocketDto.ChatMessage>): StompFrameHandler {
        override fun getPayloadType(headers: StompHeaders): Type {
            return WebSocketDto.ChatMessage::class.java
        }

        override fun handleFrame(headers: StompHeaders, payload: Any?) {
            queue.offer(payload as WebSocketDto.ChatMessage)
        }
    }

    init {
        this.describe("웹소켓 테스트") {
            context("연결") {

                it("1") {
                    val request = AuthDto.SignupRequest("fooId", "fooId",
                            "foo@wafflestudio.com", "password")

                    withContext(Dispatchers.IO) {
                        authService.signup(request)
                    }

                    val client = WebSocketStompClient(StandardWebSocketClient())
                    client.messageConverter = GsonMessageConverter()

                    val (token, _) = jwtProvider.generateToken("fooId", WebgamRoles.USER)
                    logger.info(token)

                    val httpHeaders = WebSocketHttpHeaders()

                    val headers = StompHeaders()
                    headers.set("Authorization", token)

                    val connection = client.connectAsync(URL, httpHeaders, headers, WebgamStompSessionHandler())
                    val session = withContext(Dispatchers.IO) {
                        connection.get(1000, TimeUnit.SECONDS)
                    };

                    session.subscribe("/topic/public", WebgamStompFrameHandler(messageQueue))

                    val sendHeaders = StompHeaders()
                    sendHeaders.set("Authorization", token)
                    sendHeaders.destination = "/app/send"
                    sendHeaders.contentType = APPLICATION_JSON
                    session.send(sendHeaders, WebSocketDto.ChatMessage("Hello, Websocket!"))

                    withContext(Dispatchers.IO) {
                        Thread.sleep(1000) // 이거 안 넣어주면 응답 오기 전에 테스트 끝남
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

    }
}