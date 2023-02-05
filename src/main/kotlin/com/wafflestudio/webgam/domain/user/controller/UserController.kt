package com.wafflestudio.webgam.domain.user.controller

import com.wafflestudio.webgam.domain.user.dto.UserDto.PatchRequest
import com.wafflestudio.webgam.domain.user.dto.UserDto.SimpleResponse
import com.wafflestudio.webgam.domain.user.service.UserService
import com.wafflestudio.webgam.global.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/me")
    fun getMyUserInfo(@CurrentUser myId: Long): ResponseEntity<SimpleResponse> {
        return ResponseEntity.ok(userService.getMe(myId))
    }

    @PatchMapping("/me")
    fun patchMyUserInfo(
        @CurrentUser myId: Long,
        @RequestBody @Valid request: PatchRequest
    ): ResponseEntity<SimpleResponse> {
        System.err.println(request.toString())
        return ResponseEntity.ok(userService.patchMe(myId, request))
    }

    @DeleteMapping("/me")
    fun deleteMyUserInfo(@CurrentUser myId: Long): ResponseEntity<Any> {
        userService.deleteMe(myId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}")
    fun getUserInfo(@PathVariable("id") @Positive userId: Long): ResponseEntity<SimpleResponse> {
        return ResponseEntity.ok(userService.getUserWithId(userId))
    }
}