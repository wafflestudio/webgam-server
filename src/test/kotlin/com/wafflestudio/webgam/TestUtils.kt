package com.wafflestudio.webgam

import com.wafflestudio.webgam.domain.event.model.TransitionType
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType.*
import com.wafflestudio.webgam.global.common.exception.ErrorType

class TestUtils {
    companion object {
        fun makeFieldList(vararg fields: List<Pair<Any?, Int?>>): List<Pair<List<Any?>, Pair<Int, Int?>>> {
            val main: MutableList<Pair<MutableList<Any?>, Pair<Int, Int?>>> = mutableListOf()
            val defaultValues = fields.map { it.first { (_, t) -> t == null }.first }

            for (idx in fields.indices) {
                val field = fields[idx]
                field.forEach { (d, t) ->
                    if (d == defaultValues[idx]) return@forEach
                    val copy = defaultValues.toMutableList()
                    copy[idx] = d
                    main.add(copy to ((if (t == null) -1 else idx) to t))
                }
            }

            return main
        }

        fun pathVariableIds() = listOf(
            listOf("1", "3", "5", "100").map { it to null },
            listOf("0", "-1", "-100").map { it to ErrorType.BadRequest.CONSTRAINT_VIOLATION.code() }).flatten()

        fun testData1() = listOf(
            Relation withUser "user-01"
                        withProject "project-01"
                            withPage "page-01"
                            withPage "page-02"
                                withObject "object-01" type DEFAULT
                                withObject "object-02" type DEFAULT
                                    withEvent TransitionType.DEFAULT deleted true
                                withObject "object-03" type IMAGE
                                    withEvent TransitionType.DEFAULT deleted true
                                    withEvent TransitionType.DEFAULT
                                withObject "object-04" type TEXT
                                    withEvent TransitionType.DEFAULT deleted true
                                    withEvent TransitionType.DEFAULT deleted true
                                    withEvent TransitionType.DEFAULT withNextPage "page-01"

                        withProject "project-02" deleted true
                            withPage "page-03" deleted true
                            withPage "page-04" deleted true
                                withObject "object-05" type IMAGE deleted true
                                withObject "object-06" type DEFAULT deleted true
                                    withEvent TransitionType.DEFAULT withNextPage "page-03" deleted true
                                withObject "object-07" type IMAGE deleted true
                                withObject "object-08" type TEXT deleted true
            ,
            Relation withUser "user-02" deleted true
                        withProject "project-03" deleted true
            ,
            Relation withUser "user-03"
                        withProject "project-04"
        ).map { it.build() }
    }
}