package io.aconite.parser

import io.aconite.annotations.*

interface RootApi {
    @MODULE("/modules/first/{id}")
    suspend fun first(@Path id: Long): FirstApi

    @MODULE("/modules/second-int/{named-arg}")
    suspend fun secondInt(@Path("named-arg") namedArg: String): SecondApi<Int>

    @GET("/data")
    suspend fun getData(): Data

    @PUT("/data")
    suspend fun putData(@Body data: Data)
}

interface FirstApi {
    @GET("/resources")
    suspend fun getResources(): List<String>

    @POST("/resources")
    suspend fun addResource(@Body data: String)
}

interface SecondApi<T> {
    @GET("/typed")
    suspend fun getTyped(): Complex<T>

    @PUT("/typed")
    suspend fun putTyped(@Query argument: Long, @Body data: T)

    @DELETE("/typed")
    suspend fun deleteTyped()
}

data class Data(val foo: Int, val bar: String)

@ResponseClass
data class Complex<out T>(
        @Body val body: T,
        @Header("Header") val header: String
)