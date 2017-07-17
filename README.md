Aconite
=======

Type-safe HTTP client/server framework for Kotlin inspired by [Retrofit](http://square.github.io/retrofit/).

Introduction
------------

1) Declare your HTTP API as an interface (similar to retrofit):

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

4) Generate an interface implementation for the client:

```kotlin
val client = AconiteClient(VertxHttpClient(8080, "localhost"))
val api = client.create<HelloApi>()
```

5) Make a call:

```kotlin
val response = api.hello("World")
println(response) // prints "Hello, World!"
```

6) Test the server by curl:

```bash
$ curl -XPOST http://localhost:8080/hello?name=World
Hello, World!
```

Declaring HTTP interfaces
--------------------

HTTP interface is a simple Kotlin interface with HTTP annotations on functions and arguments. To declare function, that are going to be an HTTP request, you need to select a method annotation from this list:

- `DELETE(url)`
- `GET(url)`
- `HEAD(url)`
- `OPTIONS(url)`
- `PATCH(url)`
- `POST(url)`
- `PUT(url)`

or use `HTTP(method, url)` to support custom HTTP methods. Also aconite supports inner interfaces with `MODULE(url)` annotation, that can be used to split your API into small independent parts. All arguments of the function must be annotated with one of the following annotations:

- `Body`
- `Header(name)`
- `Path(name)`
- `Query(name)`

By default, the `name` field of the annotation is equal to the function's argument name.

Here are some examples of complex interfaces:

```kotlin
data class User(val firstName: String, val lastName: String)
data class Post(val content: String, val createdAt: Date)

interface RootApi {
    @MODULE("/users/{id}") suspend fun user(@Path id: UUID): UserApi
    @MODULE("/posts/{id}") suspend fun post(@Path id: UUID): PostApi
}

interface UserApi {
    @GET suspend fun get(): User
    @PATCH suspend fun update(@Query firstName: String, @Query lastName: String)
    @GET("/posts") suspend fun getAllPosts(): Map<UUID, Post>
}

interface PostApi {
    @GET suspend fun get(): Post
    @PUT suspend fun put(@Query author: UUID, @Body content: String): Post
}

data class First(val a: Int, val b: Long)
data class Second(val c: String, val d: UUID)

interface GenericExample {
    @MODULE("/first") suspend fun first(): GenericApi<First>
    @MODULE("/second") suspend fun second(): GenericApi<Second>
}

interface GenericApi<T> {
    @GET("/{key}") suspend fun get(@Path key: String): T
    @PUT("/{key}") suspend fun put(@Path key: String, @Body value: T)
}
```

Build
-----

Aconite using gradle build tool. To build and run tests, use following command:

```bash
./gradlew clean shadowjar test
```
