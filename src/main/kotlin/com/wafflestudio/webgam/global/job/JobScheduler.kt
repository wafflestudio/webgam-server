package com.wafflestudio.webgam.global.job

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
@EnableScheduling
class JobScheduler(
    private val jobLauncher: JobLauncher,
    @Qualifier("softDeleteJob") private val softDeleteJob: Job
) {

    @Scheduled(cron="0 0 0 1 * *", zone="Asia/Seoul")
    fun runSoftDeleteJob() {
        jobLauncher.run(softDeleteJob, JobParameters())
    }
}