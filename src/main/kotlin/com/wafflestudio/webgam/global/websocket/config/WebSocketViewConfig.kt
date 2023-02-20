package com.wafflestudio.webgam.global.websocket.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.concurrent.TimeUnit


@Configuration
class WebSocketViewConfig: WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/websocket/**", "/websocket")
                .addResourceLocations("classpath:/resources/main/static/websocket/")
                .setCacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES))
    }
}