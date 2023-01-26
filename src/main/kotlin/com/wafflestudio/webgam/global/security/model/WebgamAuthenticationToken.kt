package com.wafflestudio.webgam.global.security.model

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class WebgamAuthenticationToken(
    private val userPrincipal: UserDetails,
    private val accessToken: String,
    private val authorities: MutableCollection<out GrantedAuthority> = mutableListOf()
): AbstractAuthenticationToken(authorities + userPrincipal.authorities) {
    override fun getCredentials(): Any {
        return accessToken
    }

    override fun getPrincipal(): Any {
        return userPrincipal
    }

    override fun isAuthenticated(): Boolean {
        return true
    }
}