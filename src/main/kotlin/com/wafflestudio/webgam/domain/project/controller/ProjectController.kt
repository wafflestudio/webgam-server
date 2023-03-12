package com.wafflestudio.webgam.domain.project.controller

import com.wafflestudio.webgam.domain.project.dto.ProjectDto.*
import com.wafflestudio.webgam.domain.project.service.ProjectService
import com.wafflestudio.webgam.global.common.dto.ListResponse
import com.wafflestudio.webgam.global.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.data.domain.Slice
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/api/v1/projects")
class ProjectController(
        private val projectService: ProjectService,
) {
    @GetMapping("/{id}")
    fun getProjectInfo(@CurrentUser myId: Long, @PathVariable("id") @Positive projectId: Long): ResponseEntity<DetailedResponse> {
        return ResponseEntity.ok(projectService.getProject(myId, projectId))
    }

    @GetMapping("")
    fun getProjects(
            @RequestParam(required = false, defaultValue = "0") @PositiveOrZero page: Int,
            @RequestParam(required = false, defaultValue = "10") @Positive size: Int,
    ): ResponseEntity<Slice<SimpleResponse>> {
        val projectList = projectService.getProjectList(page, size)
        return ResponseEntity.ok(projectList)
    }

    @GetMapping("/me")
    fun getMyProjects(@CurrentUser myId: Long): ResponseEntity<ListResponse<SimpleResponse>> {
        val projectList = projectService.getUserProject(myId)
        return ResponseEntity.ok(projectList)
    }

    @PostMapping("")
    fun createProject(@CurrentUser myId: Long, @RequestBody @Valid request: CreateRequest)
    : ResponseEntity<DetailedResponse> {
        System.err.println(request.toString())
        val project = projectService.createProject(myId, request)
        return ResponseEntity.ok(project)
    }

    @PatchMapping("/{id}")
    fun patchProject(@CurrentUser myId: Long, @PathVariable("id") @Positive id: Long, @RequestBody @Valid request: PatchRequest)
    : ResponseEntity<DetailedResponse> {
        System.err.println(request.toString())
        val project = projectService.patchProject(myId, id, request)
        return ResponseEntity.ok(project)
    }

    @DeleteMapping("/{id}")
    fun deleteProject(@CurrentUser myId: Long, @PathVariable("id") @Positive id: Long)
    : ResponseEntity<DetailedResponse> {
        projectService.deleteProject(myId, id)
        return ResponseEntity.ok().build()
    }
}