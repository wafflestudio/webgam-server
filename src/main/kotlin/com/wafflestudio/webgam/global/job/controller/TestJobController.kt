package com.wafflestudio.webgam.global.job.controller

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// TODO: this file will be removed before PR

@RestController
@RequestMapping("/api/v1/jobs")
class TestJobController(
    private val jobLauncher: JobLauncher,
    private val softDeleteJob: Job
) {

    @DeleteMapping("")
    fun softDeleteItems() : ResponseEntity<Any> {
        jobLauncher.run(softDeleteJob, JobParameters())
        return ResponseEntity.ok("succeed")
    }
}