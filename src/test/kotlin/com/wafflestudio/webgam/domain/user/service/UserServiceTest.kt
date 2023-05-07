package com.wafflestudio.webgam.domain.user.service

import com.wafflestudio.webgam.domain.user.dto.UserDto.PatchRequest
import com.wafflestudio.webgam.domain.user.exception.UserNotFoundException
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.inspectors.forAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@Tag("Unit-Test")
@DisplayName("UserService 단위 테스트")
class UserServiceTest: DescribeSpec() {

    companion object {
        private val user = User("fooId", "foo", "foo@wafflestudio.com", "")
        private val userRepository = mockk<UserRepository>()
        private val userService = UserService(userRepository)
    }

    init {
        this.describe("getMe 호출될 때") {
            every { userRepository.findUserById(1) } returns user

            context("현재 로그인 된 유저 아이디를 넣으면") {
                it("유저의 SimpleResponse 가 반환된다") {
                    val response = withContext(Dispatchers.IO) {
                        userService.getMe(1)
                    }

                    assertThat(response).extracting("userId").isEqualTo("fooId")
                    assertThat(response).extracting("username").isEqualTo("foo")
                    assertThat(response).extracting("email").isEqualTo("foo@wafflestudio.com")
                }
            }
        }

        this.describe("patchMe 호출될 때") {
            context("현재 로그인 된 유저 아이디와 바꿀 항목을 입력하면") {
                it("수정된 정보의 SimpleResponse 가 반환된다") {
                    val tuples = mutableListOf("new username" to "new@wafflestudio.com")

                    tuples.forAll { (username, email) ->
                        val temp = User("fooId", "foo", "foo@wafflestudio.com", "")
                        every { userRepository.findUserById(1) } returns temp

                        val response = userService.patchMe(1, PatchRequest(username, email))

                        assertThat(response).extracting("userId").isEqualTo("fooId")
                        assertThat(response).extracting("username").isEqualTo(username)
                        assertThat(response).extracting("email").isEqualTo(email)
                    }
                }

                it("값이 NULL 인 항목들은 바뀌지 않는다") {
                    val tuples = mutableListOf(
                        "new username" to null,
                        null to "new@wafflestudio.com",
                        null to null,
                    )

                    tuples.forAll { (username, email) ->
                        val temp = User("fooId", "foo", "foo@wafflestudio.com", "")
                        every { userRepository.findUserById(1) } returns temp

                        val response = userService.patchMe(1, PatchRequest(username, email))

                        assertThat(response).extracting("userId").isEqualTo("fooId")
                        assertThat(response).extracting("username").isEqualTo(username ?: "foo")
                        assertThat(response).extracting("email").isEqualTo(email ?: "foo@wafflestudio.com")
                    }
                }
            }
        }

        this.describe("deleteMe 호출 될 때") {
            val temp = User("", "", "", "")
            every { userRepository.findUserById(1) } returns temp

            context("현재 로그인 된 유저 아이디를 넣으면") {
                it("유저의 isDeleted 필드는 true 가 된다") {
                    assertThat(temp.isDeleted).isFalse()

                    withContext(Dispatchers.IO) {
                        userService.deleteMe(1)
                    }

                    assertThat(temp.isDeleted).isTrue()
                }
            }
        }

        this.describe("getUserWithId 호출 될 때") {
            context("존재하는 유저의 아이디를 넣으면") {
                every { userRepository.findUserById(1000) } returns user

                it("유저의 SimpleResponse 가 반환된다") {
                    val response = withContext(Dispatchers.IO) {
                        userService.getUserWithId(1000)
                    }

                    assertThat(response).extracting("userId").isEqualTo("fooId")
                    assertThat(response).extracting("username").isEqualTo("foo")
                    assertThat(response).extracting("email").isEqualTo("foo@wafflestudio.com")
                }
            }

            context("존재하지 않는 유저의 아이디를 넣으면") {
                every { userRepository.findUserById(1000) } returns null

                it("UserNotFoundException 예외를 던진다") {
                    assertThrows<UserNotFoundException> { userService.getUserWithId(1000) }
                }
            }

            context("존재하지만 탈퇴한 유저의 아이디를 넣으면") {
                val temp = User("", "", "", "")
                temp.delete()
                every { userRepository.findUserById(1000) } returns temp

                it("UserNotFoundException 예외를 던진다") {
                    assertThrows<UserNotFoundException> { userService.getUserWithId(1000) }
                }
            }
        }
    }
}
