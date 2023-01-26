package com.wafflestudio.webgam

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WebgamApplication

fun main(args: Array<String>) {
	runApplication<WebgamApplication>(*args)
}
