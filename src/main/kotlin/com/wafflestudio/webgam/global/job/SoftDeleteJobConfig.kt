package com.wafflestudio.webgam.global.job

import com.wafflestudio.webgam.domain.`object`.repository.PageObjectRepository
import com.wafflestudio.webgam.domain.event.repository.ObjectEventRepository
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource



@Configuration
@EnableBatchProcessing
class SoftDeleteJobConfig(
    private val jobRepository: JobRepository,
    private val dataSource: DataSource,
    private val transactionManager: PlatformTransactionManager,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val projectPageRepository: ProjectPageRepository,
    private val pageObjectRepository: PageObjectRepository,
    private val objectEventRepository: ObjectEventRepository
) {
    private final val CHUNK_SIZE = 10
    private final val DELETE_EXPIRATION_DAYS = 30

    data class DbItemIdDto(
        val id: Long
    )

    @Bean
    fun softDeleteJob(): Job {
        return JobBuilder("softDeleteJob", jobRepository)
            .start(softDeleteUserStep())
            .next(softDeleteProjectStep())
            .next(softDeleteProjectPageStep())
            .next(softDeletePageObjectStep())
            .next(softDeleteObjectEventStep())
            .build()
    }

    @Bean
    @JobScope
    fun softDeleteUserStep(): Step {
        return buildSoftDeleteStep(stepName = "softDeleteUserStep", readerName = "deleteUserReader",
            tableName = "user", repository = userRepository
        )
    }

    @Bean
    @JobScope
    fun softDeleteProjectStep(): Step {
        return buildSoftDeleteStep(stepName = "softDeleteProjectStep", readerName = "deleteProjectReader",
            tableName = "project", repository = projectRepository
        )
    }

    @Bean
    @JobScope
    fun softDeleteProjectPageStep(): Step {
        return buildSoftDeleteStep(stepName = "softDeleteProjectPageStep", readerName = "deleteProjectPageReader",
            tableName = "project_page", repository = projectPageRepository
        )
    }

    @Bean
    @JobScope
    fun softDeletePageObjectStep(): Step {
        return buildSoftDeleteStep(stepName = "softDeletePageObjectStep", readerName = "deletePageObjectReader",
        tableName = "page_object", repository = pageObjectRepository
        )
    }

    @Bean
    @JobScope
    fun softDeleteObjectEventStep(): Step {
        return buildSoftDeleteStep(stepName = "softDeleteObjectEventStep", readerName = "deleteObjectEventReader",
        tableName = "object_event", repository = objectEventRepository
        )
    }

    private fun <T> buildSoftDeleteStep(
        stepName: String, readerName: String, tableName: String, repository: JpaRepository<T, Long>
    ): Step {
        return StepBuilder(stepName, jobRepository)
            .chunk<DbItemIdDto, DbItemIdDto>(CHUNK_SIZE, transactionManager)
            .reader(deleteItemReader(tableName = tableName, readerName = readerName))
            .writer(deleteItemWriter(repository))
            .allowStartIfComplete(true)
            .build()
    }

    private fun deleteItemReader(tableName: String, readerName: String): JdbcCursorItemReader<DbItemIdDto> {
        return JdbcCursorItemReaderBuilder<DbItemIdDto>()
            .fetchSize(CHUNK_SIZE)
            .dataSource(dataSource)
            .rowMapper { rs, _ ->  DbItemIdDto(id = rs.getLong("id")) }
            .sql(getQueryOfFindDeletedItems(tableName))
            .name(readerName)
            .build()
    }

    private fun <T> deleteItemWriter(repository: JpaRepository<T, Long>): ItemWriter<DbItemIdDto> {
        return ItemWriter { it -> repository.deleteAllById(it.map { it.id }) }
    }

    private fun getQueryOfFindDeletedItems(tableName: String): String {
        return String.format(
            "select id from %s where %s.is_deleted=true and %s.deleted_at < DATE_SUB(NOW(), INTERVAL ${DELETE_EXPIRATION_DAYS} DAY)",
            tableName, tableName, tableName
        )
    }
}
