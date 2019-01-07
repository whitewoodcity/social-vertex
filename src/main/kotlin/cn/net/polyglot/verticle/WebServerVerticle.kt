/**
MIT License

Copyright (c) 2018 White Wood City

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import cn.net.polyglot.module.lowerCaseValue
import com.codahale.fastuuid.UUIDGenerator
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Cookie
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.kotlin.core.eventbus.sendAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import java.security.SecureRandom
import kotlin.random.Random

class WebServerVerticle : CoroutineVerticle() {

  override suspend fun start() {
    val httpServer = vertx.createHttpServer()
    val router = Router.router(vertx)

    val generator = UUIDGenerator(SecureRandom())

    router.route().handler(CookieHandler.create())
    router.route().handler(BodyHandler.create().setBodyLimit(1 * 1048576L))//1MB = 1048576L

    router.route().handler { routingContext ->
      if (routingContext.getCookie(SESSION_ID) == null) {
        if(Random.nextInt(100)==0) generator.reseed()
        val value = generator.generate().toString()

        val age = 31 * 24 * 3600L //31 days in seconds
        val cookie = Cookie.cookie(SESSION_ID, value)
        val path = "/" //give any suitable path
        cookie.path = path
        cookie.setMaxAge(age) //if this is not there, then a session cookie is set
        routingContext.addCookie(cookie)

        routingContext.response().isChunked = true
        routingContext.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        routingContext.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "*")
      }

      routingContext.next()
    }

    //web start

    //web end

    //im start
    router.put("/:$TYPE/:$SUBTYPE").handler { routingContext ->
      try {
        val type = routingContext.request().getParam(TYPE)
        val subtype = routingContext.request().getParam(SUBTYPE)

        val json = routingContext.bodyAsJson
          .put(TYPE, type)
          .put(SUBTYPE, subtype)
          .lowerCaseValue(ID)

        when (type) {
          FRIEND, MESSAGE -> {
            vertx.eventBus().send(IMMessageVerticle::class.java.name, json)
            routingContext.response().end()
          }
          else -> launch {
            val result =
              vertx.eventBus().sendAwait<JsonObject>(IMMessageVerticle::class.java.name, json).body()
            routingContext.response().end(result.toString())
          }
        }
      } catch (e: Exception) {
        routingContext.response().end(e.message)
        return@handler
      }
    }
    //im end

    httpServer.requestHandler(router).listen(config.getInteger(HTTP_PORT)) {
      if (it.succeeded()) {
        println("${this::class.java.name} is deployed")
      } else {
        println("${this::class.java.name} fail to deploy")
      }
    }
  }

}

