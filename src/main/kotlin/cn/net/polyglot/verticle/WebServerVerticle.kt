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
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Cookie
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine
import io.vertx.kotlin.core.eventbus.sendAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.ext.web.common.template.renderAwait
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
    router.getWithRegex("/.*(\\.htm|\\.css|\\.text|\\.png|\\.jpg|\\.gif|\\.jpeg|\\.mp3|\\.avi)")
      .handler(StaticHandler.create("./"))//如果是静态文件，直接交由static handler处理，注意只接受http方法为get的请求

    router.get("/*").handler { routingContext:RoutingContext ->
      val path = routingContext.request().path()
      when(path){
        "/","/index" -> routingContext.reroute("/index.htm")
        else -> routingContext.next()
      }
    }

    val routingHandler = { routingContext:RoutingContext ->

      val requestJson = JsonObject()

      val path = routingContext.request().path()
      val httpMethod = routingContext.request().method()
      val cookies = routingContext.cookies()
      val headers = routingContext.request().headers()
      val params = routingContext.queryParams()
      val attributes = routingContext.request().formAttributes()

      requestJson.put(PATH, path)

      when(httpMethod){
        HttpMethod.GET -> requestJson.put(HTTP_METHOD, GET)
        HttpMethod.POST -> requestJson.put(HTTP_METHOD, POST)
        HttpMethod.PUT -> requestJson.put(HTTP_METHOD, PUT)
        else -> requestJson.put(HTTP_METHOD, OTHER)
      }

      var json = JsonObject()
      for(cookie in cookies){
        json.put(cookie.name, cookie.value)
      }
      requestJson.put(COOKIES, json)

      json = JsonObject()
      for(header in headers){
        json.put(header.key, header.value)
      }
      requestJson.put(HEADERS, json)

      json = JsonObject()
      var iterator = params.iterator()
      while(iterator.hasNext()){
        val i = iterator.next()
        json.put(i.key,i.value)
      }
      requestJson.put(QUERY_PARAM, json)

      json = JsonObject()
      iterator = attributes.iterator()
      println(attributes)
      while(iterator.hasNext()){
        val i = iterator.next()
        if(json.containsKey(i.key)){
          var index = 0
          while(json.containsKey("${i.key}$index")){
            index++
          }
          json.put("${i.key}$index", i.value)
        }else
          json.put(i.key,i.value)
      }
      requestJson.put(FORM_ATTRIBUTES, json)

//      launch {
//        val templatePath = dispatch(path, requestJson)
//        routingContext.put(TEMPLATE_PATH, templatePath)
//        routingContext.next()
//      }

      Unit
    }

    router.get("/*").handler(routingHandler)
    router.post("/*").handler(routingHandler)

    //render part
    val engine = ThymeleafTemplateEngine.create(vertx)
    val templateHandler = { routingContext:RoutingContext ->
      launch {
        val buffer = engine.renderAwait(JsonObject(), routingContext.get(TEMPLATE_PATH))
        routingContext.response().end(buffer)
      }
      Unit
    }

    router.post("/*").handler(templateHandler)
    router.get("/*").handler(templateHandler)
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

  private suspend fun dispatch(path:String, jsonObject: JsonObject):String{
    val result:String

    val verticleAddress = verticleAddressDispatch(path)
    if(verticleAddress!=null){
      val responseJson = vertx.eventBus().sendAwait<JsonObject>(verticleAddress, jsonObject).body()
      result = responseJson.getString(TEMPLATE_PATH)
    }else{
      result = ""
    }

    return result
  }

  private fun verticleAddressDispatch(path:String):String?{
    return when(path){
      "/" -> "cn.net.polyglot.verticle.SampleVerticle"
      else -> null
    }
  }

}

