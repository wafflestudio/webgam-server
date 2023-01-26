package com.wafflestudio.webgam.global.security.jwt

import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.global.security.exception.InvalidJwtException
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.security.model.WebgamAuthenticationToken
import com.wafflestudio.webgam.global.security.model.WebgamRoles
import com.wafflestudio.webgam.global.security.model.WebgamRoles.*
import com.wafflestudio.webgam.global.security.service.UserPrincipalDetailsService
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

@Component
class JwtProvider(
    private val userPrincipalDetailsService: UserPrincipalDetailsService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${jwt.secret-key}")
    private val secretKey = ""

    private lateinit var signingKey: Key
    @PostConstruct
    fun init() {
        signingKey = Keys.hmacShaKeyFor(secretKey.encodeToByteArray())
    }

    companion object {
        const val header: String = "Authorization"
        private const val tokenPrefix: String = "Bearer "
        const val refreshTokenValidTime = 2 * 7 * 24 * 60 * 60 * 1000L
        private const val accessTokenValidTime = 2 * 60 * 60 * 1000L
        private val admin = User("admin", "admin", "admin@webgam.wafflestudio", "")
        private val developer = User("dev", "dev", "dev@webgam.wafflestudio", "")
        private val adminAuthority = SimpleGrantedAuthority("ADMIN")
        private val devAuthority = SimpleGrantedAuthority("DEV")
        private val docsAuthority = SimpleGrantedAuthority("DOCS")
    }

    private fun removeTokenPrefix(tokenWithPrefix: String): String {
        return tokenWithPrefix.replace(tokenPrefix, "").trim { it <= ' ' }
    }

    private fun getClaimsFromToken(token: String): Claims {
        return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(removeTokenPrefix(token)).body
    }

    fun getAuthenticationFromToken(token: String): Authentication {
        val claims = getClaimsFromToken(token)

        return when (WebgamRoles.from(claims["role"])) {
            DEV -> WebgamAuthenticationToken(UserPrincipal(developer), token, mutableListOf(devAuthority, docsAuthority))
            ADMIN -> WebgamAuthenticationToken(UserPrincipal(admin), token, mutableListOf(adminAuthority, docsAuthority))
            USER -> WebgamAuthenticationToken(userPrincipalDetailsService.loadUserByUsername(claims.subject), token)
            else -> throw InvalidJwtException("Invalid role in JWT token.")
        }
    }

    fun generateToken(subject: String, role: WebgamRoles?, vararg pairs: Pair<String, String>): Pair<String, String> {
        val claims = Jwts.claims().setSubject(subject)
        val now = Date()

        val refreshToken = tokenPrefix + Jwts.builder().setClaims(claims).setIssuedAt(now)
            .setExpiration(Date(now.time + refreshTokenValidTime))
            .signWith(signingKey, SignatureAlgorithm.HS256).compact()

        claims["role"] = role
        for (pair in pairs) claims[pair.first] = pair.second
        val accessToken = tokenPrefix + Jwts.builder().setClaims(claims).setIssuedAt(now)
            .setExpiration(Date(now.time + accessTokenValidTime))
            .signWith(signingKey, SignatureAlgorithm.HS256).compact()

        return accessToken to refreshToken
    }

    fun generateToken(refreshToken: String): Pair<String, String> {
        validate(refreshToken)

        val claims = getClaimsFromToken(refreshToken)

        return generateToken(
            claims.subject,
            WebgamRoles.from(claims["role"]),
            *claims.keys.filter { it != "sub" && it != "iat" && it != "exp" }
                .map<String, Pair<String, String>> { it to claims[it] as String }.toTypedArray()
        )
    }

    fun validate(token: String) {
        if (!token.startsWith(tokenPrefix)) throw InvalidJwtException("Token does not match type Bearer.")

        try {
            getClaimsFromToken(token)
        } catch (e: ExpiredJwtException) {
            throw InvalidJwtException("Expired JWT token.")
        } catch (e: UnsupportedJwtException) {
            throw InvalidJwtException("Unsupported JWT token.")
        } catch (e: MalformedJwtException) {
            throw InvalidJwtException("Invalid JWT token.")
        }
    }
}