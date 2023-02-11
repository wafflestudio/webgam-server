package com.wafflestudio.webgam

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
    }
}