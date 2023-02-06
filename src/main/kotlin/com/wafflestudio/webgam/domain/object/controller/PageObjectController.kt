package com.wafflestudio.webgam.domain.`object`.controller

import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto.*
import com.wafflestudio.webgam.domain.`object`.service.PageObjectService
import com.wafflestudio.webgam.global.common.dto.ListResponse
import com.wafflestudio.webgam.global.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/api/v1/objects")
class PageObjectController(
    private val projectObjectService: PageObjectService,
) {
    @GetMapping
    fun listObjects(
        @CurrentUser myId: Long,
        @RequestParam("project-id") @NotNull @Positive projectId: Long?,
    ): ResponseEntity<ListResponse<DetailedResponse>> {
        return ResponseEntity.ok(projectObjectService.listProjectObjects(myId, projectId!!))
    }

    @PostMapping
    fun createObject(
        @CurrentUser myId: Long,
        @RequestBody @Valid request: CreateRequest,
    ): ResponseEntity<SimpleResponse> {
        return ResponseEntity.ok(projectObjectService.createObject(myId, request))
    }

    @GetMapping("/{id}")
    fun getObject(
        @CurrentUser myId: Long,
        @PathVariable("id") @Positive objectId: Long,
    ): ResponseEntity<DetailedResponse> {
        return ResponseEntity.ok(projectObjectService.getObject(myId, objectId))
    }

    @PatchMapping("/{id}")
    fun patchObject(
        @CurrentUser myId: Long,
        @PathVariable("id") @Positive objectId: Long,
        @RequestBody @Valid request: PatchRequest,
    ): ResponseEntity<DetailedResponse> {
        return ResponseEntity.ok(projectObjectService.modifyObject(myId, objectId, request))
    }

    @DeleteMapping("/{id}")
    fun deleteObject(
        @CurrentUser myId: Long,
        @PathVariable("id") @Positive objectId: Long,
    ): ResponseEntity<Any> {
        projectObjectService.deleteObject(myId, objectId)
        return ResponseEntity.ok().build()
    }
}