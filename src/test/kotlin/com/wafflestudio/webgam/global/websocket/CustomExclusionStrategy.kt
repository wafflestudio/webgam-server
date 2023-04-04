package com.wafflestudio.webgam.global.websocket

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.wafflestudio.webgam.global.common.dto.TimeTraceEntityDto

class CustomExclusionStrategy: ExclusionStrategy {

    override fun shouldSkipField(f: FieldAttributes?): Boolean {
        val sourceClass = f?.declaringClass!! == TimeTraceEntityDto.Response::class.java
        val idField = f.name.equals("id") && sourceClass
        val createdAtField = f.name.equals("createdAt")
        val createdByField = f.name.equals("createdBy")
        val modifiedAtField = f.name.equals("modifiedAt")
        val modifiedByField = f.name.equals("modifiedBy")


        if(idField || createdAtField || createdByField || modifiedAtField || modifiedByField){
            return true
        }
        return false
    }

    override fun shouldSkipClass(clazz: Class<*>?): Boolean {
        return false
    }
}