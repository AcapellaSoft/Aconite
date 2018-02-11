package io.aconite.parser

import io.aconite.utils.UrlTemplate
import io.aconite.utils.resolve
import org.junit.Assert
import org.junit.Test
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.functions
import kotlin.reflect.full.primaryConstructor

class ParserTest {
    @Test
    fun testAll() {
        val parser = ModuleParser()
        val got = parser.parse(RootApi::class)

        val firstDesc = ModuleDesc(FirstApi::class, listOf(
                HttpMethodDesc(
                        url = UrlTemplate("/resources"),
                        method = "GET",
                        arguments = listOf(),
                        response = BodyResponseDesc(type<FirstApi, List<*>>(String::class.createType())),
                        function = fn<FirstApi>("getResources")
                ),
                HttpMethodDesc(
                        url = UrlTemplate("/resources"),
                        method = "POST",
                        arguments = listOf(
                                BodyArgumentDesc(fn<FirstApi>("addResource").param("data"), false)
                        ),
                        response = BodyResponseDesc(type<RootApi, Unit>()),
                        function = fn<FirstApi>("addResource")
                )
        ))

        val secondDescInt = ModuleDesc(SecondApi::class, listOf(
                HttpMethodDesc(
                        url = UrlTemplate("/typed"),
                        method = "DELETE",
                        arguments = listOf(),
                        response = BodyResponseDesc(Unit::class.createType()),
                        function = fn<SecondApi<*>>("deleteTyped", Int::class.createType())
                ),
                HttpMethodDesc(
                        url = UrlTemplate("/typed"),
                        method = "GET",
                        arguments = listOf(),
                        response = ComplexResponseDesc(
                                type = type<SecondApi<*>, Complex<*>>(Int::class.createType()),
                                constructor = ctor<SecondApi<*>>(Int::class.createType()),
                                fields = listOf(
                                        BodyFieldDesc(prop<SecondApi<*>, Complex<*>>("body", Int::class.createType())),
                                        HeaderFieldDesc(prop<SecondApi<*>, Complex<*>>("header", Int::class.createType()), "Header")
                                )
                        ),
                        function = fn<SecondApi<*>>("getTyped", Int::class.createType())
                ),
                HttpMethodDesc(
                        url = UrlTemplate("/typed"),
                        method = "PUT",
                        arguments = listOf(
                                QueryArgumentDesc(fn<SecondApi<*>>("putTyped", Int::class.createType()).param("argument"), false, "argument"),
                                BodyArgumentDesc(fn<SecondApi<*>>("putTyped", Int::class.createType()).param("data"), false)
                        ),
                        response = BodyResponseDesc(Unit::class.createType()),
                        function = fn<SecondApi<*>>("putTyped", Int::class.createType())
                )
        ))

        val expected = ModuleDesc(RootApi::class, listOf(
                ModuleMethodDesc(
                        url = UrlTemplate("/modules/second-int/{named-arg}"),
                        arguments = listOf(
                                PathArgumentDesc(fn<RootApi>("secondInt").param("namedArg"), false, "named-arg")
                        ),
                        response = secondDescInt,
                        function = fn<RootApi>("secondInt")
                ),
                ModuleMethodDesc(
                        url = UrlTemplate("/modules/first/{id}"),
                        arguments = listOf(
                                PathArgumentDesc(fn<RootApi>("first").param("id"), false, "id")
                        ),
                        response = firstDesc,
                        function = fn<RootApi>("first")
                ),
                HttpMethodDesc(
                        url = UrlTemplate("/data"),
                        method = "GET",
                        arguments = listOf(),
                        response = BodyResponseDesc(type<RootApi, Data>()),
                        function = fn<RootApi>("getData")
                ),
                HttpMethodDesc(
                        url = UrlTemplate("/data"),
                        method = "PUT",
                        arguments = listOf(
                                BodyArgumentDesc(fn<RootApi>("putData").param("data"), false)
                        ),
                        response = BodyResponseDesc(type<RootApi, Unit>()),
                        function = fn<RootApi>("putData")
                )
        ))

        Assert.assertEquals(expected, got)
    }

    private inline fun <reified T: Any> fn(name: String, vararg arguments: KType) = resolve(
            T::class.createType(arguments.map { KTypeProjection.invariant(it) }),
            T::class.functions.first { it.name == name }
    )

    private inline fun <reified T: Any> ctor(vararg arguments: KType) = resolve(
            T::class.createType(arguments.map { KTypeProjection.invariant(it) }),
            T::class.primaryConstructor!!
    )

    private inline fun <reified P: Any, reified T: Any> prop(name: String, vararg arguments: KType) = resolve(
            T::class.createType(arguments.map { KTypeProjection.invariant(it) }),
            T::class.declaredMemberProperties.first { it.name == name }
    )

    private fun KFunction<*>.param(name: String) = this.parameters.first { it.name == name }

    private inline fun <reified P: Any, reified T: Any> type() = resolve(P::class.createType(), T::class.createType())

    private inline fun <reified P: Any, reified T: Any> type(vararg arguments: KType) =
            resolve(P::class.createType(), T::class.createType(arguments.map { KTypeProjection.invariant(it) }))
}