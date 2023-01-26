package com.wafflestudio.webgam

import com.wafflestudio.webgam.RestDocsAttributeKeys.Companion.KEY_DEFAULT_VALUE
import com.wafflestudio.webgam.RestDocsAttributeKeys.Companion.KEY_FORMAT
import com.wafflestudio.webgam.RestDocsAttributeKeys.Companion.KEY_SAMPLE
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.restdocs.snippet.Attributes.Attribute
import org.springframework.restdocs.snippet.Snippet

internal class RestDocsUtils {
    companion object {
        fun getDocumentRequest() = preprocessRequest(
            modifyUris()
                .scheme("https")
                .host("docs.api.com")
                .removePort(),
            prettyPrint()
        )!!

        fun getDocumentResponse() = preprocessResponse(
            modifyHeaders()
                .removeMatching("Vary")
                .removeMatching("X-Content-Type-Options")
                .removeMatching("X-XSS-Protection")
                .removeMatching("Cache-Control")
                .removeMatching("Pragma")
                .removeMatching("Expires")
                .removeMatching("X-Frame-Options"),
            prettyPrint()
        )!!

        fun requestBody(vararg field: Field): Snippet {
            return PayloadDocumentation.requestFields(field.map { it.descriptor })
        }

        fun responseBody(vararg field: Field): Snippet {
            return PayloadDocumentation.responseFields(field.map { it.descriptor })
        }

        fun defaultValue(value: String) = Attribute(KEY_DEFAULT_VALUE, value)
        fun customFormat(value: String) = Attribute(KEY_FORMAT, value)
        fun customSample(value: String) = Attribute(KEY_SAMPLE, value)

        fun emptyDefaultValue() = defaultValue("")
        fun emptyFormat() = customFormat("")
        fun emptySample() = customSample("")

        fun enumFormat(enums: Collection<Any>): String {
            return enums.joinToString(separator = "|")
        }

        const val DATE_FORMAT = "yyyy-MM-dd"
        const val DATETIME_FORMAT = "yyyy-MM-dd hh:mm:ss.SSS"
    }
}

internal class RestDocsAttributeKeys {
    companion object {
        const val KEY_DEFAULT_VALUE = "default_value"
        const val KEY_FORMAT = "format"
        const val KEY_SAMPLE = "sample"
    }
}