package com.wafflestudio.webgam

import com.wafflestudio.webgam.RestDocsAttributeKeys.Companion.KEY_DEFAULT_VALUE
import com.wafflestudio.webgam.RestDocsAttributeKeys.Companion.KEY_FORMAT
import com.wafflestudio.webgam.RestDocsAttributeKeys.Companion.KEY_SAMPLE
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import kotlin.reflect.KClass

/**
* @see <a href="https://toss.tech/article/kotlin-dsl-restdocs">Kotlin으로 DSL 만들기: 반복적이고 지루한 REST Docs 벗어나기</a>
*/

sealed class DocsFieldType(
    val type: JsonFieldType
)

object ARRAY: DocsFieldType(JsonFieldType.ARRAY)
object BOOLEAN: DocsFieldType(JsonFieldType.BOOLEAN)
object OBJECT: DocsFieldType(JsonFieldType.OBJECT)
object NUMBER: DocsFieldType(JsonFieldType.NUMBER)
object NULL: DocsFieldType(JsonFieldType.NULL)
object STRING: DocsFieldType(JsonFieldType.STRING)
object ANY: DocsFieldType(JsonFieldType.VARIES)
object DATE: DocsFieldType(JsonFieldType.STRING)
object DATETIME: DocsFieldType(JsonFieldType.STRING)

open class Field(
    val descriptor: FieldDescriptor,
) {
    val isIgnored: Boolean = descriptor.isIgnored
    val isOptional: Boolean = descriptor.isOptional

    protected open var default: String
        get() = descriptor.attributes.getOrDefault(KEY_DEFAULT_VALUE, "") as String
        set(value) {
            descriptor.attributes(RestDocsUtils.defaultValue(value))
        }

    protected open var format: String
        get() = descriptor.attributes.getOrDefault(KEY_FORMAT, "") as String
        set(value) {
            descriptor.attributes(RestDocsUtils.customFormat(value))
        }

    protected open var sample: String
        get() = descriptor.attributes.getOrDefault(KEY_SAMPLE, "") as String
        set(value) {
            descriptor.attributes(RestDocsUtils.customSample(value))
        }

    open infix fun means(value: String): Field {
        descriptor.description(value)
        return this
    }

    open infix fun attributes(block: Field.() -> Unit): Field {
        block()
        return this
    }

    open infix fun withDefaultValue(value: String): Field {
        this.default = value
        return this
    }

    open infix fun formattedAs(value: String): Field {
        this.format = value
        return this
    }

    open infix fun example(value: String): Field {
        this.sample = value
        return this
    }

    open infix fun isOptional(value: Boolean): Field {
        if (value) descriptor.optional()
        return this
    }

    open infix fun isIgnored(value: Boolean): Field {
        if (value) descriptor.ignored()
        return this
    }
}

data class ENUM<T : Enum<T>>(val enums: Collection<T>) : DocsFieldType(JsonFieldType.STRING) {
    constructor(clazz: KClass<T>) : this(clazz.java.enumConstants.asList())
}

infix fun String.type(docsFieldType: DocsFieldType): Field {
    val field = createField(this, docsFieldType.type, false)
    when (docsFieldType) {
        is DATE -> field formattedAs RestDocsUtils.DATE_FORMAT
        is DATETIME -> field formattedAs RestDocsUtils.DATETIME_FORMAT
        else -> {}
    }
    return field
}

infix fun <T : Enum<T>> String.type(enumFieldType: ENUM<T>): Field {
    val field = createField(this, JsonFieldType.STRING, false)
    field formattedAs RestDocsUtils.enumFormat(enumFieldType.enums)
    return field
}

private fun createField(value: String, type: JsonFieldType, optional: Boolean): Field {
    val descriptor = PayloadDocumentation.fieldWithPath(value)
        .type(type)
        .attributes(RestDocsUtils.emptySample(), RestDocsUtils.emptyFormat(), RestDocsUtils.emptyDefaultValue())
        .description("")

    if (optional) descriptor.optional()

    return Field(descriptor)
}