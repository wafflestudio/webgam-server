package com.wafflestudio.webgam.domain

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ping")
class PingTestController {
    @GetMapping
    fun pingTest(): ResponseEntity<String> {
        return ResponseEntity.ok("pong")
    }
}