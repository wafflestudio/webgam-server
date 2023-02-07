package com.wafflestudio.webgam.domain.page.controller

import com.wafflestudio.webgam.domain.page.service.ProjectPageService
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto.DetailedResponse
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto.SimpleResponse
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto.CreateRequest
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto.PatchRequest
import com.wafflestudio.webgam.domain.project.service.ProjectService
import com.wafflestudio.webgam.global.common.dto.ListResponse
import com.wafflestudio.webgam.global.security.CurrentUser
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/api/v1/project/{project_id}/edit")
class ProjectPageController (
        private val projectPageService: ProjectPageService,
){
    @GetMapping("")
    fun getProjectPageInfo(
            @CurrentUser myId:Long,
            @PathVariable("project_id") projectId: Long,
            @RequestParam(required = true) name: String)
    : ResponseEntity<DetailedResponse> {
        val projectPage = projectPageService.getProjectPage(myId, projectId, name)
        return ResponseEntity.ok(projectPage)
    }

    @GetMapping("")
    fun getProjectPagesInfo(@CurrentUser myId:Long, @PathVariable("project_id") projectId: Long)
    : ResponseEntity<ListResponse<SimpleResponse>>{
        val projectPages = projectPageService.getProjectPages(myId, projectId)
        return ResponseEntity.ok(projectPages)
    }


    @GetMapping("")
    fun createProjectPage(
            @CurrentUser myId: Long,
            @PathVariable("project_id") id: Long,
            @RequestBody @Valid request: CreateRequest
    ): ResponseEntity<DetailedResponse> {
        val projectPage = projectPageService.createProjectPage(myId, id, request)
        return ResponseEntity.ok(projectPage)
    }

    @PatchMapping("")
    fun patchProjectPage(
            @CurrentUser myId: Long,
            @PathVariable("project_id") id: Long,
            @RequestParam(required=true) name: String,
            @RequestBody @Valid request: PatchRequest
    ): ResponseEntity<DetailedResponse> {
        val projectPage = projectPageService.patchProjectPage(myId, id, name, request)
        return ResponseEntity.ok(projectPage)
    }

    @DeleteMapping("")
    fun deleteProjectPage(
            @CurrentUser myId: Long,
            @PathVariable("project_id") id: Long,
            @RequestParam(required=true) name: String
    )
    : ResponseEntity<SimpleResponse> {
        val projectPage = projectPageService.deleteProjectPage(myId, id, name)
        return ResponseEntity.ok(projectPage)
    }

}