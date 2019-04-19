package cn.net.polyglot.verticle.web

import cn.net.polyglot.config.*
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.sendAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch

abstract class ServletVerticle : CoroutineVerticle() {

  override suspend fun start() {
    start(this::class.java.asSubclass(this::class.java).name)//缺省以实际继承类名作为地址
    println("${this::class.java.asSubclass(this::class.java).name} is deployed")
  }

  protected fun start(address: String) {
    vertx.eventBus().consumer<JsonObject>(address) {
      val reqJson = it.body()
      val session = Session(reqJson.getJsonObject(COOKIES).getString(SESSION_ID))

      launch {
        when (reqJson.getString(HTTP_METHOD)) {
          POST -> {
            it.reply(doPost(reqJson, session).toJson())
          }
          GET -> {
            it.reply(doGet(reqJson, session).toJson())
          }
          PUT -> {
            it.reply(doPut(reqJson, session).toJson())
          }
          else -> it.reply(JsonObject().put(JSON_BODY, "Http Method is not specified"))
        }
      }
    }
  }

  //Service Proxy of the Session Verticle
  inner class Session(private val id: String) {

    fun put(key: String, value: String?) {
      vertx.eventBus().send(SessionVerticle::class.java.name,
        JsonObject().put(ACTION, PUT).put(SESSION_ID, id).put(INFORMATION, JsonObject().put(key, value))
      )
    }

    fun remove(key: String) {
      vertx.eventBus().send(SessionVerticle::class.java.name,
        JsonObject().put(ACTION, REMOVE).put(SESSION_ID, id).put(INFORMATION, key)
      )
    }

    suspend fun get(key: String): String? {
      val result = vertx.eventBus().sendAwait<String>(SessionVerticle::class.java.name,
        JsonObject().put(ACTION, GET).put(SESSION_ID, id).put(INFORMATION, key)
      )
      return result.body()
    }
  }

  enum class ResponseType {
    EMPTY_RESPONSE, TEMPLATE, FILE, JSON
  }

  inner class Response(val type: ResponseType, val path: String = "index.htm", private val values: JsonObject = JsonObject()) {
    constructor(json: JsonObject = JsonObject()) : this(ResponseType.JSON, "index.htm", json)
    constructor() : this(ResponseType.EMPTY_RESPONSE)
    constructor(filePath: String) : this(ResponseType.FILE, filePath)
    constructor(templatePath: String, values: JsonObject) : this(ResponseType.TEMPLATE, templatePath, values)

    fun toJson(): JsonObject {
      return when (type) {
        ResponseType.TEMPLATE -> JsonObject().put(TEMPLATE_PATH, path).put(VALUES, values)
        ResponseType.FILE -> JsonObject().put(FILE_PATH, path)
        ResponseType.JSON -> JsonObject().put(RESPONSE_JSON, values)
        else -> JsonObject().put(EMPTY_RESPONSE, true)
      }
    }
  }

  open suspend fun doGet(json: JsonObject, session: Session): Response {
    return Response()
  }

  open suspend fun doPost(json: JsonObject, session: Session): Response {
    return Response()
  }

  open suspend fun doPut(json: JsonObject, session: Session): Response {
    return Response()
  }
}
