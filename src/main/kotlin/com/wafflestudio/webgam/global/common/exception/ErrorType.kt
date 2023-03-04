package com.wafflestudio.webgam.global.common.exception

enum class ErrorType {
    ;
    enum class BadRequest(private val code: Int): Error {
        DEFAULT(0),
        INVALID_FIELD(1),
        NO_REFRESH_TOKEN(2),
        CONSTRAINT_VIOLATION(3),
        JSON_PARSE_ERROR(4),
        PARAMETER_TYPE_MISMATCH(5),
        PAGE_IN_OTHER_PROJECT(400),
        ;

        override fun code(): Int {
            return this.code
        }
    }

    enum class Unauthorized(private val code: Int): Error {
        DEFAULT(1000),
        LOGIN_FAIL(1001),
        INVALID_JWT(1002),
        ;

        override fun code(): Int {
            return this.code
        }
    }

    enum class Forbidden(private val code: Int): Error {
        DEFAULT(3000),
        NO_ACCESS(3001),
        NON_ACCESSIBLE_PROJECT(3100),
        NON_ACCESSIBLE_PROJECT_PAGE(3200),
        NON_ACCESSIBLE_PAGE_OBJECT(3300),
        NON_ACCESSIBLE_OBJECT_EVENT(3400),
        ;

        override fun code(): Int {
            return this.code
        }
    }

    enum class NotFound(private val code: Int): Error {
        DEFAULT(4000),
        USER_NOT_FOUND(4001),
        PROJECT_NOT_FOUND(4100),
        PROJECT_PAGE_NOT_FOUND(4200),
        PAGE_OBJECT_NOT_FOUND(4300),
        OBJECT_EVENT_NOT_FOUND(4400),
        ;

        override fun code(): Int {
            return this.code
        }
    }

    enum class Conflict(private val code: Int): Error {
        DEFAULT(9000),
        DUPLICATE_USER_IDENTIFIER(9001),
        ONLY_SINGLE_EVENT_PER_OBJECT(9400),
        ;

        override fun code(): Int {
            return this.code
        }
    }

    enum class ServerError(private val code: Int): Error {
        DEFAULT(10000),
        ;

        override fun code(): Int {
            return this.code
        }
    }
}
