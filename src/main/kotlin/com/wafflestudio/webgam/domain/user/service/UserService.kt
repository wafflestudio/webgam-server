package com.wafflestudio.webgam.domain.user.service

import com.wafflestudio.webgam.domain.user.dto.UserDto.PatchRequest
import com.wafflestudio.webgam.domain.user.dto.UserDto.SimpleResponse
import com.wafflestudio.webgam.domain.user.exception.UserNotFoundException
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {
    fun getMe(myId: Long): SimpleResponse {
        val me = userRepository.findUserById(myId)!!
        return SimpleResponse(me)
    }

    @Transactional
    fun patchMe(myId: Long, request: PatchRequest): SimpleResponse {
        val me = userRepository.findUserById(myId)!!

        request.username ?.let { me.username = it }
        request.email ?.let { me.email = it }

        return SimpleResponse(me)
    }

    @Transactional
    fun deleteMe(myId: Long) {
        val me = userRepository.findUserById(myId)!!
        me.isDeleted = true
    }

    fun getUserWithId(userId: Long): SimpleResponse {
        val user = userRepository.findUserById(userId) ?: throw UserNotFoundException(userId)
        if (user.isDeleted) throw UserNotFoundException(userId)

        return SimpleResponse(user)
    }
}