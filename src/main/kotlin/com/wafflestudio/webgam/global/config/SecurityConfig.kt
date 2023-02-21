package com.wafflestudio.webgam.global.config

import com.wafflestudio.webgam.global.security.controller.WebgamAccessDeniedHandler
import com.wafflestudio.webgam.global.security.controller.WebgamAuthenticationEntryPoint
import com.wafflestudio.webgam.global.security.jwt.JwtAuthenticationFilter
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import com.wafflestudio.webgam.global.security.service.UserPrincipalDetailsService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@EnableWebSecurity
@Configuration
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val userPrincipalDetailsService: UserPrincipalDetailsService,
    private val webgamAuthenticationEntryPoint: WebgamAuthenticationEntryPoint,
    private val webgamAccessDeniedHandler: WebgamAccessDeniedHandler,
) {
    companion object {
        private val CORS_WHITELIST: MutableList<String> = mutableListOf(
            "http://webgam-dev.s3-website.ap-northeast-2.amazonaws.com:3000",
            "http://localhost:3000",
        )
        private val GET_WHITELIST: Array<String> = arrayOf("/ping", "/api/v1/project", "/ws/**", "/ws", "/websocket")
        private val POST_WHITELIST: Array<String> = arrayOf("/signup", "/login/**", "/logout", "/refresh", "/ws/**", "/ws")
    }

    @Value("\${spring.profiles.active}")
    private val activeProfile = ""

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .httpBasic().disable()
            .csrf().disable()
            .logout().disable()
            .cors().configurationSource(corsConfigurationSource())
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(webgamAuthenticationEntryPoint)
            .accessDeniedHandler(webgamAccessDeniedHandler)
            .and()
            .addFilter(JwtAuthenticationFilter(authenticationManager(), jwtProvider))
            .authorizeHttpRequests()
            .requestMatchers(HttpMethod.GET, *GET_WHITELIST).permitAll()
            .requestMatchers(HttpMethod.POST, *POST_WHITELIST).permitAll()
            .requestMatchers(HttpMethod.GET, "/auth-ping").authenticated()
            .requestMatchers("/api/v1/**").hasAuthority("USER")
            .requestMatchers(HttpMethod.GET, "/docs/**") attachAuthorityAccordingTo activeProfile

        return http.build()
    }

    infix fun AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl.attachAuthorityAccordingTo(activeProfile: String) {
        if (activeProfile == "prod") this.hasAuthority("DOCS")
        else this.permitAll()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowCredentials = true
        config.allowedOriginPatterns = CORS_WHITELIST
        config.addAllowedHeader("*")
        config.addAllowedMethod("*")
        config.addExposedHeader("Authorization")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

    @Bean
    fun authenticationManager(): AuthenticationManager {
        val daoAuthenticationProvider = DaoAuthenticationProvider()
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder())
        daoAuthenticationProvider.setUserDetailsService(userPrincipalDetailsService)

        return ProviderManager(daoAuthenticationProvider)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder()
    }
}