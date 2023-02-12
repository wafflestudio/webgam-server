package com.wafflestudio.webgam.domain.event.controller

import com.wafflestudio.webgam.domain.event.dto.ObjectEventDto.*
import com.wafflestudio.webgam.domain.event.service.ObjectEventService
import com.wafflestudio.webgam.global.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/api/v1/events")
class ObjectEventController(
    private val objectEventService: ObjectEventService,
) {
    @PostMapping
    fun createObjectEvent(
        @CurrentUser myId: Long,
        @RequestBody @Valid request: CreateRequest
    ): ResponseEntity<SimpleResponse> {
        return ResponseEntity.ok(objectEventService.createEvent(myId, request))
    }

    @GetMapping("/{id}")
    fun getObjectEvent(
        @CurrentUser myId: Long,
        @PathVariable("id") @Positive eventId: Long,
    ): ResponseEntity<SimpleResponse> {
        return ResponseEntity.ok(objectEventService.getEvent(myId, eventId))
    }

    @PatchMapping("/{id}")
    fun updateObjectEvent(
        @CurrentUser myId: Long,
        @PathVariable("id") @Positive eventId: Long,
        @RequestBody @Valid request: PatchRequest,
    ): ResponseEntity<SimpleResponse> {
        return ResponseEntity.ok(objectEventService.updateEvent(myId, eventId, request))
    }

    @DeleteMapping("/{id}")
    fun deleteObjectEvent(
        @CurrentUser myId: Long,
        @PathVariable("id") @Positive eventId: Long,
    ): ResponseEntity<Any> {
        objectEventService.deleteEvent(myId, eventId)
        return ResponseEntity.ok().build()
    }
}