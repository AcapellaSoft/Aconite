package io.aconite.server.complextest

import java.util.*

fun <K, V> mutableSortedMap(vararg entries: Pair<K, V>) = TreeMap(entries.toMap())

val USER_1: UUID = UUID.randomUUID()
val DEVICE_1: UUID = UUID.randomUUID()
val DEVICE_2: UUID = UUID.randomUUID()
val SERVICE_1: UUID = UUID.randomUUID()

class RootImpl: RootApi {
    val commandQueues = mapOf(
            USER_1 to mapOf(
                    "seq-1" to mutableSortedMap(
                            0L to Command("foo", "text"),
                            1L to Command("bar", 123),
                            4L to Command("baz", false)
                    )
            ),
            DEVICE_1 to mapOf(
                    "seq-1" to mutableSortedMap(
                            43L to Command("a", 1),
                            67L to Command("b", 2),
                            99L to Command("c", 3)
                    )
            ),
            DEVICE_2 to mapOf(
                    "seq-1" to mutableSortedMap(
                            100L to Command("d", "e"),
                            200L to Command("d", "f")
                    )
            ),
            SERVICE_1 to mapOf(
                    "seq-1" to mutableSortedMap(
                            100L to Command("cmd-1", "value-1"),
                            200L to Command("cmd-2", "value-2")
                    )
            )
    )

    suspend override fun devices(deviceId: UUID) = DevicesImpl(deviceId, this)
    suspend override fun users(userId: UUID) = UsersImpl(userId, this)
    suspend override fun services(serviceId: UUID) = ServicesImpl(serviceId, this)
}

abstract class EntityImpl<T: Entity>(val id: UUID, val root: RootImpl): EntityApi<T> {
    suspend override fun commands(queueName: String) = OrderedMapImpl(root.commandQueues[id]!![queueName]!!)
    suspend override fun sequences(sequenceName: String) = TODO()
}

class DevicesImpl(id: UUID, root: RootImpl): EntityImpl<Device>(id, root), DevicesApi {
    suspend override fun get(): Device {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun edit(entity: Device) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class UsersImpl(id: UUID, root: RootImpl): EntityImpl<User>(id, root), UsersApi {
    suspend override fun getAllDevices(): Map<UUID, Device> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun get(): User {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun edit(entity: User) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class ServicesImpl(id: UUID, root: RootImpl): EntityImpl<Service>(id, root), ServicesApi {
    suspend override fun getAllDevices(): Map<UUID, Device> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun get(): Service {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun edit(entity: Service) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class OrderedMapImpl<E>(val map: SortedMap<Long, E?>): OrderedMapApi<E> {
    suspend override fun getAll() = map
    suspend override fun get(key: Long) = map[key]
    suspend override fun put(key: Long, value: E?) { map[key] = value }
}