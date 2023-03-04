package com.wafflestudio.webgam.domain.page.controller

import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto.*
import com.wafflestudio.webgam.domain.page.service.ProjectPageService
import com.wafflestudio.webgam.global.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/api/v1/pages")
class ProjectPageController (
        private val projectPageService: ProjectPageService,
){
    @GetMapping("/{id}")
    fun getProjectPageInfo(
            @CurrentUser myId:Long,
            @PathVariable("id") @Positive projectId: Long
    )
    : ResponseEntity<DetailedResponse> {
        val projectPage = projectPageService.getProjectPage(myId, projectId)
        return ResponseEntity.ok(projectPage)
    }

    //TODO get all pages in project?


    @PostMapping("")
    fun createProjectPage(
            @CurrentUser myId: Long,
            @RequestBody @Valid request: CreateRequest
    ): ResponseEntity<DetailedResponse> {
        val projectPage = projectPageService.createProjectPage(myId, request)
        return ResponseEntity.ok(projectPage)
    }

    @PatchMapping("/{id}")
    fun patchProjectPage(
            @CurrentUser myId: Long,
            @PathVariable("id") @Positive id: Long,
            @RequestBody @Valid request: PatchRequest
    ): ResponseEntity<DetailedResponse> {
        val projectPage = projectPageService.patchProjectPage(myId, id, request)
        return ResponseEntity.ok(projectPage)
    }

    @DeleteMapping("/{id}")
    fun deleteProjectPage(
            @CurrentUser myId: Long,
            @PathVariable("id") @Positive id: Long
    )
    : ResponseEntity<Any> {
        projectPageService.deleteProjectPage(myId, id)
        return ResponseEntity.ok().build()
    }

}