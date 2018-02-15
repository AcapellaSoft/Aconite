package io.aconite.parser

import kotlin.reflect.KClass
import kotlin.reflect.KType

data class ModuleDesc(
        val clazz: KClass<*>,
        val type: KType,
        val methods: List<MethodDesc>
)