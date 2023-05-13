package com.wafflestudio.webgam.domain.project

import com.google.gson.reflect.TypeToken
import com.wafflestudio.webgam.domain.project.dto.ProjectDto
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
@DisplayName("Project Websocket 통합 테스트")
class ProjectWebsocketDescribeSpec(
        @Autowired private val jwtProvider: JwtProvider,
        @Autowired private val userRepository: UserRepository
): WebSocketDescribeSpec(userRepository) {

    private inline fun <reified T> parseArray(json: String, typeToken: Type): T {
        return gson.fromJson<T>(json, typeToken)
    }

    init {
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
                    sendHeaders.contentType = MimeTypeUtils.APPLICATION_JSON
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

    }


}