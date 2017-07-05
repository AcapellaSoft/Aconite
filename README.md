Aconite
=======

Type-safe HTTP client/server framework for Kotlin.

Introduction
------------

1) Declare your HTTP API as an interface:

```kotlin
interface HelloApi {
    @POST("/hello") suspend fun hello(@Query name: String): String
}
```

2) Create an interface implementation for the server:

```kotlin
class HelloImpl : HelloApi {
    override suspend fun hello(name: String) = "Hello, $name!"
}
```

3) Create and run the server using default vertx handler

```kotlin
fun main(args: Array<String>) {
    val server = AconiteServer()
    server.register(HelloImpl(), HelloApi::class)
    VertxHandler.runServer(server, 8080)
}
```

4) Generate client interface implementation:

```kotlin
val client = AconiteClient()
val api = client.create(HelloApi::class)
```

5) Make a call:

```kotlin
run(CommonPool) {
    val response = api.hello("World")
    println(response) // prints "Hello, World!"
}
```