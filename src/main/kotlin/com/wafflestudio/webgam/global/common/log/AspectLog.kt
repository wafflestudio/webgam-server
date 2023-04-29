package com.wafflestudio.webgam.global.common.log

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
class AspectLog {
    val log = Logger.logger

    /**
     * Pointcut of controllers
     */
    @Pointcut("execution(public * com.wafflestudio.webgam..*.controller.*.*(..)) || execution(public * com.wafflestudio.webgam.domain.PingTestController.*(..))")
    fun controllerCuts() {}

    /**
     * Logging called controller's REST API method, uri, and execution time
     */
    @OptIn(ExperimentalTime::class)
    @Around("controllerCuts()")
    fun controllerExecutionTime(joinPoint: ProceedingJoinPoint): Any? {

        val (result, executionTime) = measureTimedValue {
            joinPoint.proceed()
        }
        val servletContainer = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes

        log.info {
            "\n======================\n" +
                    "\t Method : ${servletContainer.request.method}\n" +
                    "\t Uri : ${servletContainer.request.requestURI}\n" +
                    "\t Execution time : ${executionTime}\n" +
            "======================\n"
        }
        return result
    }
}