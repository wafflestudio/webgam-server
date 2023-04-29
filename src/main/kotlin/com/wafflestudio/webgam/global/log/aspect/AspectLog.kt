package com.wafflestudio.webgam.global.log.aspect

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.webgam.global.common.dto.ErrorResponse
import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException
import com.wafflestudio.webgam.global.log.Logger
import com.wafflestudio.webgam.global.log.dto.RequestResponseLog
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.util.ContentCachingRequestWrapper
import software.amazon.awssdk.http.HttpStatusCode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Aspect
@Component
class AspectLog (
        private val objectMapper: ObjectMapper,
) {
    val log = Logger.logger

    /**
     * Pointcut of controllers
     */
    @Pointcut("execution(public * com.wafflestudio.webgam..*.controller.*.*(..)) || execution(public * com.wafflestudio.webgam.domain.PingTestController.*(..))")
    fun controllerCuts() {}

    @OptIn(ExperimentalTime::class)
    @Around("controllerCuts()")
    fun requestResponseLogging(joinPoint: ProceedingJoinPoint): Any? {
        val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request as ContentCachingRequestWrapper // FIXME: Type casting doesn't work
        val traceId = request.getAttribute("traceId") as UUID
        val className = joinPoint.signature.declaringTypeName
        val methodName = joinPoint.signature.name
        val params = getParameters(request)

        val requestResponseLog = RequestResponseLog(
                traceId,
                className,
                httpMethod = request.method!!,
                uri = request.requestURI!!,
                method = methodName,
                params,
                logTime = LocalDateTime.now(),
                requestBody = objectMapper.readTree(request.contentAsByteArray),
        )

        return try {
            val (result, executionTime) = measureTimedValue {
                joinPoint.proceed()
            }
            val successLog = requestResponseLog.copy(
                    responseBody = when (result) {
                        is ResponseEntity<*> -> result.body
                        else -> "{}"
                    },
                    elapsedTime = executionTime.toLong(DurationUnit.MILLISECONDS),
            )
            log.info { objectMapper.writeValueAsString(successLog) }
            result
        } catch (e: Exception) {
            val failLog = requestResponseLog.copy(
                    responseBody = ErrorResponse(
                        errorCode = HttpStatusCode.INTERNAL_SERVER_ERROR,
                        errorMessage = "Duh",
                        detail = "DDuuhh",
                    ),
                    stackTrace = e.stackTraceToString(),
            )
            log.error { objectMapper.writeValueAsString(failLog) }
            throw e
        }
    }

    // TODO: Handle when handled exception occurs
    // FIXME: Handling for unhandled exception occurs

    private fun getParameters(request: ContentCachingRequestWrapper): Map<String, String> {
        val paramsNames = request.parameterNames.toList()
        val replacedParamsNames = paramsNames.map {
            it.replace("\\.", "-")
        }
        return (replacedParamsNames zip paramsNames).associate {
            it.first to request.getParameter(it.second)
        }
    }
}