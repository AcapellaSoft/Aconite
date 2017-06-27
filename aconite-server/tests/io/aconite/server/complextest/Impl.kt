package io.aconite.server.complextest

import java.util.*

fun <K, V> mutableSortedMap(vararg entries: Pair<K, V>) = TreeMap(entries.toMap())

class RootImpl: RootApi {
    val commandQueues = mapOf(
            "user-1" to mapOf(
                    "seq-1" to mutableSortedMap(
                            "0" to Command("foo", "text"),
                            "1" to Command("bar", 123),
                            "4" to Command("baz", false)
                    )
            ),
            "device-1" to mapOf(
                    "seq-1" to mutableSortedMap(
                            "43" to Command("a", 1),
                            "67" to Command("b", 2),
                            "99" to Command("c", 3)
                    )
            ),
            "device-2" to mapOf(
                    "seq-1" to mutableSortedMap(
                            "100" to Command("d", "e"),
                            "200" to Command("d", "f")
                    )
            ),
            "service-1" to mapOf(
                    "seq-1" to mutableSortedMap(
                            "100" to Command("cmd-1", "value-1"),
                            "200" to Command("cmd-2", "value-2")
                    )
            )
    )

    suspend override fun devices(deviceId: String) = DevicesImpl(deviceId, this)
    suspend override fun users(userId: String) = UsersImpl(userId, this)
    suspend override fun services(serviceId: String) = ServicesImpl(serviceId, this)
}

abstract class EntityImpl<T: Entity>(val id: String, val root: RootImpl): EntityApi<T> {
    suspend override fun commands(queueName: String) = OrderedMapImpl(root.commandQueues[id]!![queueName]!!)
    suspend override fun sequences(sequenceName: String) = TODO()
}

class DevicesImpl(id: String, root: RootImpl): EntityImpl<Device>(id, root), DevicesApi {
    suspend override fun get(): Device {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun edit(entity: Device) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class UsersImpl(id: String, root: RootImpl): EntityImpl<User>(id, root), UsersApi {
    suspend override fun getAllDevices(): Map<String, Device> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun get(): User {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun edit(entity: User) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class ServicesImpl(id: String, root: RootImpl): EntityImpl<Service>(id, root), ServicesApi {
    suspend override fun getAllDevices(): Map<String, Device> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun get(): Service {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun edit(entity: Service) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class OrderedMapImpl<E>(val map: SortedMap<String, E?>): OrderedMapApi<E> {
    suspend override fun getAll() = map
    suspend override fun get(key: String) = map[key]
    suspend override fun put(key: String, value: E?) { map[key] = value }
}