package io.aconite.server.complextest

import io.aconite.annotations.*
import java.util.*

interface RootApi {
    @MODULE("/devices/{deviceId}") suspend fun devices(@Path deviceId: String): DevicesApi
    @MODULE("/users/{userId}") suspend fun users(@Path userId: String): UsersApi
    @MODULE("/services/{serviceId}") suspend fun services(@Path serviceId: String): ServicesApi
}

interface EntityApi<T: Entity> {
    @GET suspend fun get(): T
    @PATCH suspend fun edit(@Body entity: T)
    @MODULE("/command-queues/{queueName}") suspend fun commands(@Path queueName: String): OrderedMapApi<Command>
    @MODULE("/sequences/{sequenceName}") suspend fun sequences(@Path sequenceName: String): OrderedMapApi<Any>
}

interface DevicesApi: EntityApi<Device>

interface UsersApi: EntityApi<User> {
    @GET("/devices") suspend fun getAllDevices(): Map<String, Device>
}

interface ServicesApi: EntityApi<Service> {
    @GET("/devices") suspend fun getAllDevices(): Map<String, Device>
}

interface OrderedMapApi<E> {
    @GET suspend fun getAll(): SortedMap<String, E?>
    @GET("/{key}") suspend fun get(@Path key: String): E?
    @PUT("/{key}") suspend fun put(@Path key: String, @Body value: E?)
}

interface Entity

data class Device(
        val serviceId: String,
        val ownerId: String
): Entity

data class User(
        val login: String,
        val firstName: String,
        val lastName: String
): Entity

data class Service(
        val name: String
): Entity

data class Command(
        val name: String,
        val payload: Any?
)