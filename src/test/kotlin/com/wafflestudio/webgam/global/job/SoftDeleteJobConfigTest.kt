package com.wafflestudio.webgam.global.job

import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.common.model.BaseTimeTraceLazyDeletedEntity
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.slf4j.LoggerFactory
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime


@ActiveProfiles("test")
@SpringBootTest(properties = ["spring.profiles.active:test"])
@SpringBatchTest
@DisplayName("SoftDeleteJob 테스트")
class SoftDeleteJobConfigTest(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository
): BehaviorSpec() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        userRepository.deleteAll()
        projectRepository.deleteAll()
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
            createUser(1, isDeleted = false, deletedOld = false)
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
            createUser(1, isDeleted = false, deletedOld = false)
            createUser(2, isDeleted = true, deletedOld = false)

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
            val user1 = createUser(1, isDeleted = false, deletedOld = false)
            createUser(2, isDeleted = true, deletedOld = true)

            When("job 실행하면") {
                val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)
                Then("오래된 유저 1명이 사라진다") {
                    jobExecution.exitStatus shouldBe ExitStatus.COMPLETED
                    userRepository.count() shouldBe 1
                    userRepository.findAll()[0].id shouldBe user1.id
                }
            }
        }

        this.Given("정상 유저 1명과 삭제된 최근 프로젝트 1개") {
            val jobParameters = jobLauncherTestUtils.uniqueJobParameters
            val user = createUser(1, isDeleted = false, deletedOld = false)
            createProject(user, isDeleted = true, deletedOld = false)
            When("job 실행하면") {
                val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)
                Then("아무것도 지워지지 않는다") {
                    jobExecution.exitStatus shouldBe ExitStatus.COMPLETED
                    userRepository.count() shouldBe 1
                    projectRepository.count() shouldBe 1
                }
            }
        }

        this.Given("정상 유저 1명과 삭제된 오래된 프로젝트 1개, 삭제되지 않은 프로젝트 1개") {
            val jobParameters = jobLauncherTestUtils.uniqueJobParameters
            val user1 = createUser(1, isDeleted = false, deletedOld = false)

            createProject(user1, isDeleted = true, deletedOld = true)
            val project2 = createProject(user1, isDeleted = true, deletedOld = false)

            When("job 실행하면") {
                val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)
                Then("삭제된 오래된 프로젝트만 지워진다") {
                    jobExecution.exitStatus shouldBe ExitStatus.COMPLETED
                    userRepository.count() shouldBe 1
                    projectRepository.count() shouldBe 1
                    projectRepository.findAll()[0].id shouldBe project2.id
                }
            }
        }

        this.Given("오래전 삭제된 유저 1명과 삭제된 오래된 프로젝트 1개, 삭제되지 않은 프로젝트 1개") {
            val jobParameters = jobLauncherTestUtils.uniqueJobParameters
            val user1 = createUser(1, isDeleted = true, deletedOld = true)

            createProject(user1, isDeleted = true, deletedOld = true)
            createProject(user1, isDeleted = false, deletedOld = false)

            When("job 실행하면") {
                val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)
                Then("유저가 삭제되면서 프로젝트도 전부 삭제된다") {
                    jobExecution.exitStatus shouldBe ExitStatus.COMPLETED
                    userRepository.count() shouldBe 0
                    projectRepository.count() shouldBe 0
                }
            }
        }
    }

    private fun setDeleted(entity: BaseTimeTraceLazyDeletedEntity, isDeleted: Boolean, deletedOld: Boolean) {
        if (deletedOld) isDeleted shouldBe true

        entity.isDeleted = isDeleted
        if (isDeleted) {
            if (deletedOld) {
                entity.deletedAt = LocalDateTime.now().minusDays(31)
            } else {
                entity.deletedAt = LocalDateTime.now()
            }
        }
    }

    private fun createUser(id: Int, isDeleted:Boolean, deletedOld: Boolean) : User {
        val user = User(userId = "user${id}", username = "user${id}", email = "user${id}@gmail.com", password = "test")
        setDeleted(user, isDeleted, deletedOld)
        return userRepository.save(user)
    }

    private fun createProject(user: User, isDeleted: Boolean, deletedOld: Boolean) : Project {
        val project = Project(owner = user, title = "title")
        setDeleted(project, isDeleted, deletedOld)
        return projectRepository.save(project)
    }
}