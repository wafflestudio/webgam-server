package com.wafflestudio.webgam.global.job

import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.slf4j.LoggerFactory
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.JobRepositoryTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime


@SpringBootTest
@SpringBatchTest
@DisplayName("SoftDeleteJob 테스트")
@ActiveProfiles("test")
class SoftDeleteJobConfigTest(
    @Autowired private val jobLauncherTestUtils: JobLauncherTestUtils,
    @Autowired private val jobRepositoryTestUtils: JobRepositoryTestUtils,
    private val userRepository: UserRepository
): BehaviorSpec() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        userRepository.deleteAll()
    }

    init {
        this.Given("데이터가 없을 때") {
            val jobParameters = jobLauncherTestUtils.uniqueJobParameters
            When("job 실행하면") {
                val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)
                Then("아무변화 없이 COMPLETED 된다") {
                    jobExecution.exitStatus shouldBe ExitStatus.COMPLETED
                }
            }
        }

        this.Given("신규 유저가 한명 있을 때") {
            val jobParameters = jobLauncherTestUtils.uniqueJobParameters
            createNewUser(1, isDeleted = false)
            When("job 실행하면") {
                val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)
                Then("유저 수가 유지된다") {
                    jobExecution.exitStatus shouldBe ExitStatus.COMPLETED
                    userRepository.count() shouldBe 1
                }
            }
        }

        this.Given("정상 유저 1명과 삭제된 유저이지만 오래되지 않은 유저 한명") {
            val jobParameters = jobLauncherTestUtils.uniqueJobParameters
            createNewUser(1, isDeleted = false)
            createNewUser(2, isDeleted = true)
            When("job 실행하면") {
                val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)
                Then("유저 수가 유지된다") {
                    jobExecution.exitStatus shouldBe ExitStatus.COMPLETED
                    userRepository.count() shouldBe 2
                }
            }
        }

        this.Given("정상 유저 1명과 삭제된 오래된 유저 한명") {
            val jobParameters = jobLauncherTestUtils.uniqueJobParameters
            createNewUser(1, isDeleted = false)
            createOldUser(2, isDeleted = true)
            When("job 실행하면") {
                val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)
                Then("오래된 유저 1명이 사라진다") {
                    jobExecution.exitStatus shouldBe ExitStatus.COMPLETED
                    userRepository.count() shouldBe 1
                }
            }
        }
    }

    private fun createUser(id: Int, isDeleted: Boolean) : User {
        val user = User(userId = "user${id}", username = "user${id}", email = "user${id}@gmail.com", password = "test")
        user.isDeleted = isDeleted
        return user
    }

    private fun createNewUser(id: Int, isDeleted:Boolean) {
        val user = createUser(id, isDeleted)
        if (isDeleted) user.deletedAt = LocalDateTime.now()
        userRepository.save(user)
    }

    private fun createOldUser(id: Int, isDeleted: Boolean) {
        val user = createUser(id, isDeleted)
        if (isDeleted) user.deletedAt = LocalDateTime.now().minusDays(31)
        userRepository.save(user)
    }
}