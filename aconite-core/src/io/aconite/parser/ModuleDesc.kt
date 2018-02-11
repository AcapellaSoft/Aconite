package io.aconite.parser

import kotlin.reflect.KClass

data class ModuleDesc(
        val clazz: KClass<*>,
        val methods: List<MethodDesc>
)