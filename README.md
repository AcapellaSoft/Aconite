Aconite
=======

Type-safe HTTP client/server framework for Kotlin.

Introduction
------------

1) Declare your HTTP API as an interface (similar to [retrofit](http://square.github.io/retrofit/)):

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
val server = AconiteServer()
server.register(HelloImpl(), HelloApi::class)
VertxHandler.runServer(server, 8080)
```

4) Generate an interface implementation for the client (not yet implemented):

```kotlin
val client = AconiteClient("http://localhost:8080")
val api = client.create(HelloApi::class)
```

5) Make a call (not yet implemented):

```kotlin
run(CommonPool) {
    val response = api.hello("World")
    println(response) // prints "Hello, World!"
}
```

or test the server by curl:

```bash
$ curl -XPOST http://localhost:8080/hello?name=World
Hello, World!
```
