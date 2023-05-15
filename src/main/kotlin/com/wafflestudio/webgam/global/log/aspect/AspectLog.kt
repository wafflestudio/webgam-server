package com.wafflestudio.webgam.global.log.aspect

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Aspect
@Component
class AspectLog (
) {
    /**
     * Pointcut of controllers
     */
    @Pointcut("execution(public * com.wafflestudio.webgam..*.controller.*.*(..)) || execution(public * com.wafflestudio.webgam.domain.PingTestController.*(..))")
    fun controllerCuts() {}

    /**
     * Add class name, method name, and execution time to HttpServletRequest
     */
    @OptIn(ExperimentalTime::class)
    @Around("controllerCuts()")
    fun addLogInfoToHttpServletRequest(joinPoint: ProceedingJoinPoint): Any? {
        val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes)
                .request

        request.run {
            setAttribute("className", joinPoint.signature.declaringTypeName)
            setAttribute("methodName", joinPoint.signature.name)
        }

        val (result, executionTime) = measureTimedValue {
            joinPoint.proceed()
        }
        request.setAttribute("elapsedTime", executionTime)

        return result
    }
}