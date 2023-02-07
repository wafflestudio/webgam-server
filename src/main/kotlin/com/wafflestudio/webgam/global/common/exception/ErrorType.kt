package com.wafflestudio.webgam.global.common.exception

enum class ErrorType {
    ;
    enum class BadRequest(private val code: Int): Error {
        DEFAULT(0),
        INVALID_FIELD(1),
        NO_REFRESH_TOKEN(2),
        CONSTRAINT_VIOLATION(3),
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
        NON_ACCESSIBLE_PROJECT(3002)
        ;

        override fun code(): Int {
            return this.code
        }
    }

    enum class NotFound(private val code: Int): Error {
        DEFAULT(4000),
        USER_NOT_FOUND(4001),
        PROJECT_NOT_FOUND(4002)
        ;

        override fun code(): Int {
            return this.code
        }
    }

    enum class Conflict(private val code: Int): Error {
        DEFAULT(9000),
        DUPLICATE_USER_IDENTIFIER(9001),
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
