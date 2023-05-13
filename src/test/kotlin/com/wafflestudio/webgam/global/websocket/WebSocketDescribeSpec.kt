package com.wafflestudio.webgam.global.websocket

import com.google.gson.*
import com.wafflestudio.webgam.TestUtils
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
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
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Transactional
@ActiveProfiles("test")
@Tag("Integration-Test")
@DisplayName("WebSocket 통합 테스트")
class WebSocketDescribeSpec(
        @Autowired private val userRepository: UserRepository
) : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    companion object {
        const val URL = "ws://localhost:8080/ws"
        val messageQueue = LinkedBlockingDeque<WebSocketDto<*>>()
        val client = WebSocketStompClient(StandardWebSocketClient())


        val gson = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeDeserializer())
                .setExclusionStrategies(CustomExclusionStrategy())
                .create()


        val data = TestUtils.docTestData()
        val user = data.first()
        val auth = WebgamAuthenticationToken(UserPrincipal(user), "")
        val project = user.projects.first()
        val page = project.pages.first()
        val pageObject = page.objects.first { it.event == null }
        val event = page.objects.first().event!!
        val nextPage = project.pages.first()


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
}