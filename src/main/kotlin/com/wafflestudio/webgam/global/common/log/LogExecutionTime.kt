package com.wafflestudio.webgam.global.common.log

import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.stereotype.Component

@Aspect
@Component
class LogChecker {
    val log = Logger.logger

    @Pointcut("execution(public * com.wafflestudio.webgam..*.controller.*.*(..)) || execution(public * com.wafflestudio.webgam.domain.PingTestController.*(..))")
    fun controllerCuts() {}

    @Around("controllerCuts()")
    fun controllerTime(jointPoint: ProceedingJoinPoint): Any? {
        log.info {"Hello ${jointPoint.signature.toShortString()}"}
        val result = jointPoint.proceed()
        log.info {"Bye ${jointPoint.signature.toShortString()}"}
        return result
    }
}