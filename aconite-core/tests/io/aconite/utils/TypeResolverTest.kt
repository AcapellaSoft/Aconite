@file:Suppress("unused")

package io.aconite.utils

import org.junit.Assert
import org.junit.Test
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

class TypeResolverTest {

    interface Data

    data class DataA(val a: Int, val b: String): Data
    data class DataC(val e: Int, val f: String): Data

    open class DataD<out T: Any>(val g: Float, val h: T): Data

    abstract class DataB(g: Float, h: String): DataD<String>(g, h) {
        abstract fun <R> unresolved(): R
    }

    interface Api {
        fun moduleA(): FirstModule<DataA>
        fun moduleB(): FirstModule<DataB>
    }

    interface FirstModule<T: Data> {
        fun get(): T
        fun getAll(): List<T>
        fun getArray(): Array<T>
        fun set(t: T)
        fun innerModuleC(): SecondModule<DataC, T>
        fun innerModuleD(): InheritSecondModule<T>
    }

    interface SecondModule<T: Data, U> {
        fun getT(): T
        fun getU(): U
        fun set(items: Map<T, U>)
    }

    interface InheritSecondModule<U: Data>: SecondModule<DataD<String>, Array<U>>

    fun cls(type: KType?) = (type!!.classifier as KClass<*>).java

    @Test
    fun test_moduleA_get() {
        val moduleA = Api::class
                .members
                .first { it.name == "moduleA" }
                .returnType

        val get = FirstModule::class
                .members
                .first { it.name == "get" }
                .returnType

        val resolved = resolve(moduleA, get)

        Assert.assertEquals(DataA::class.java, cls(resolved))
    }

    @Test
    fun test_moduleA_getAll() {
        val moduleA = Api::class
                .members
                .first { it.name == "moduleA" }
                .returnType

        val getAll = FirstModule::class
                .members
                .first { it.name == "getAll" }
                .returnType

        val resolved = resolve(moduleA, getAll)

        Assert.assertEquals(List::class.java, cls(resolved))
        Assert.assertEquals(DataA::class.java, cls(resolved.arguments[0].type))
    }

    @Test
    fun test_moduleA_getArray() {
        val moduleA = Api::class
                .members
                .first { it.name == "moduleA" }
                .returnType

        val getArray = FirstModule::class
                .members
                .first { it.name == "getArray" }
                .returnType

        val resolved = resolve(moduleA, getArray)

        Assert.assertEquals(Array<DataA>::class.java, cls(resolved))
        Assert.assertEquals(DataA::class.java, cls(resolved.arguments[0].type))
    }

    @Test
    fun test_moduleA_innerModuleC() {
        val moduleA = Api::class
                .members
                .first { it.name == "moduleA" }
                .returnType

        val innerModuleC = FirstModule::class
                .members
                .first { it.name == "innerModuleC" }
                .returnType

        val resolved = resolve(moduleA, innerModuleC)

        Assert.assertEquals(SecondModule::class.java, cls(resolved))
        Assert.assertEquals(DataC::class.java, cls(resolved.arguments[0].type))
        Assert.assertEquals(DataA::class.java, cls(resolved.arguments[1].type))
    }

    @Test
    fun test_moduleA_innerModuleC_getT() {
        val moduleA = Api::class
                .members
                .first { it.name == "moduleA" }
                .returnType

        val innerModuleC = FirstModule::class
                .members
                .first { it.name == "innerModuleC" }
                .returnType

        val getT = SecondModule::class
                .members
                .first { it.name == "getT" }
                .returnType

        val resolved = resolve(resolve(moduleA, innerModuleC), getT)

        Assert.assertEquals(DataC::class.java, cls(resolved))
    }

    @Test
    fun test_moduleA_innerModuleC_getU() {
        val moduleA = Api::class
                .members
                .first { it.name == "moduleA" }
                .returnType

        val innerModuleC = FirstModule::class
                .members
                .first { it.name == "innerModuleC" }
                .returnType

        val getU = SecondModule::class
                .members
                .first { it.name == "getU" }
                .returnType

        val resolved = resolve(resolve(moduleA, innerModuleC), getU)

        Assert.assertEquals(DataA::class.java, cls(resolved))
    }

    @Test
    fun test_moduleA_innerModuleC_set() {
        val moduleA = Api::class
                .members
                .first { it.name == "moduleA" }
                .returnType

        val innerModuleC = FirstModule::class
                .members
                .first { it.name == "innerModuleC" }
                .returnType

        val set = SecondModule::class
                .members
                .first { it.name == "set" }
                .parameters[1].type

        val resolved = resolve(resolve(moduleA, innerModuleC), set)

        Assert.assertEquals(Map::class.java, cls(resolved))
        Assert.assertEquals(DataC::class.java, cls(resolved.arguments[0].type))
        Assert.assertEquals(DataA::class.java, cls(resolved.arguments[1].type))
    }

    @Test
    fun test_moduleA_innerModuleD() {
        val moduleA = Api::class
                .members
                .first { it.name == "moduleA" }
                .returnType

        val innerModuleD = FirstModule::class
                .members
                .first { it.name == "innerModuleD" }
                .returnType

        val resolved = resolve(moduleA, innerModuleD)

        Assert.assertEquals(InheritSecondModule::class.java, cls(resolved))
        Assert.assertEquals(DataA::class.java, cls(resolved.arguments[0].type))
    }

    @Test
    fun test_moduleA_innerModuleD_getT() {
        val moduleA = Api::class
                .members
                .first { it.name == "moduleA" }
                .returnType

        val innerModuleD = FirstModule::class
                .members
                .first { it.name == "innerModuleD" }
                .returnType

        val getT = InheritSecondModule::class
                .members
                .first { it.name == "getT" }
                .returnType

        val resolved = resolve(resolve(moduleA, innerModuleD), getT)

        Assert.assertEquals(DataD::class.java, cls(resolved))
        Assert.assertEquals(String::class.java, cls(resolved.arguments[0].type))
    }

    @Test
    fun test_moduleA_innerModuleD_getU() {
        val moduleA = Api::class
                .members
                .first { it.name == "moduleA" }
                .returnType

        val innerModuleD = FirstModule::class
                .members
                .first { it.name == "innerModuleD" }
                .returnType

        val getU = InheritSecondModule::class
                .members
                .first { it.name == "getU" }
                .returnType

        val resolved = resolve(resolve(moduleA, innerModuleD), getU)

        Assert.assertEquals(Array<DataA>::class.java, cls(resolved))
        Assert.assertEquals(DataA::class.java, cls(resolved.arguments[0].type))
    }

    @Test
    fun test_moduleA_innerModuleD_set() {
        val moduleA = Api::class
                .members
                .first { it.name == "moduleA" }
                .returnType

        val innerModuleD = FirstModule::class
                .members
                .first { it.name == "innerModuleD" }
                .returnType

        val set = InheritSecondModule::class
                .members
                .first { it.name == "set" }
                .parameters[1].type

        val resolved = resolve(resolve(moduleA, innerModuleD), set)

        Assert.assertEquals(Map::class.java, cls(resolved))
        Assert.assertEquals(DataD::class.java, cls(resolved.arguments[0].type))
        Assert.assertEquals(String::class.java, cls(resolved.arguments[0].type!!.arguments[0].type))
        Assert.assertEquals(Array<DataA>::class.java, cls(resolved.arguments[1].type))
        Assert.assertEquals(DataA::class.java, cls(resolved.arguments[1].type!!.arguments[0].type))
    }

    @Test
    fun test_moduleB_get_getH() {
        val moduleA = Api::class
                .members
                .first { it.name == "moduleB" }
                .returnType

        val get = FirstModule::class
                .members
                .first { it.name == "get" }
                .returnType

        val getH = DataB::class
                .members
                .first { it.name == "h" }
                .returnType

        val resolved = resolve(resolve(moduleA, get), getH)

        Assert.assertEquals(String::class.java, cls(resolved))
    }

    @Test
    fun test_moduleB_get_unresolved() {
        val moduleA = Api::class
                .members
                .first { it.name == "moduleB" }
                .returnType

        val get = FirstModule::class
                .members
                .first { it.name == "get" }
                .returnType

        val unresolved = DataB::class
                .members
                .first { it.name == "unresolved" }
                .returnType

        val resolved = resolve(resolve(moduleA, get), unresolved).classifier as KTypeParameter

        Assert.assertEquals("R", resolved.name)
    }

    @Test
    fun testResolveFunction_moduleA_innerModuleC() {
        val moduleA = Api::moduleA.returnType
        val fn = FirstModule<*>::innerModuleC
        val resolved = resolve(moduleA, fn)
        Assert.assertEquals(SecondModule::class.java, cls(resolved.returnType))
        Assert.assertEquals(DataC::class.java, cls(resolved.returnType.arguments[0].type))
        Assert.assertEquals(DataA::class.java, cls(resolved.returnType.arguments[1].type))
    }

    @Test
    fun testResolveFunction_moduleA_set() {
        val moduleA = Api::moduleA.returnType
        val fn = FirstModule<*>::set
        val resolved = resolve(moduleA, fn)
        Assert.assertEquals(DataA::class.java, cls(resolved.parameters[1].type))
    }

    @Test
    fun testSimpleTypeToJava() {
        val type = Long::class.createType()
        Assert.assertEquals(Long::class.java, type.toJavaType())
    }

    @Test
    fun testParametrizedTypeToJava() {
        val type = Map::class.createType(listOf(
                KTypeProjection.invariant(Long::class.createType()),
                KTypeProjection.invariant(String::class.createType())
        ))
        val javaType = type.toJavaType() as ParameterizedType
        Assert.assertEquals(Map::class.java, javaType.rawType)
        Assert.assertEquals(2, javaType.actualTypeArguments.size)
        Assert.assertEquals(Long::class.java, javaType.actualTypeArguments[0])
        Assert.assertEquals(String::class.java, javaType.actualTypeArguments[1])
    }

    @Test
    fun testComplexParametrizedTypeToJava() {
        val type = Map::class.createType(listOf(
                KTypeProjection.invariant(Set::class.createType(listOf(
                        KTypeProjection.invariant(Long::class.createType())
                ))),
                KTypeProjection.invariant(List::class.createType(listOf(
                        KTypeProjection.invariant(String::class.createType())
                )))
        ))
        val javaType = type.toJavaType() as ParameterizedType
        Assert.assertEquals(Map::class.java, javaType.rawType)
        Assert.assertEquals(2, javaType.actualTypeArguments.size)

        val keyType = javaType.actualTypeArguments[0] as ParameterizedType
        Assert.assertEquals(Set::class.java, keyType.rawType)
        Assert.assertEquals(1, keyType.actualTypeArguments.size)
        Assert.assertEquals(Long::class.java, keyType.actualTypeArguments[0])

        val valueType = javaType.actualTypeArguments[1] as ParameterizedType
        Assert.assertEquals(List::class.java, valueType.rawType)
        Assert.assertEquals(1, valueType.actualTypeArguments.size)
        Assert.assertEquals(String::class.java, valueType.actualTypeArguments[0])
    }
}

