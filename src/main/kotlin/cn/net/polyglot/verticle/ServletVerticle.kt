package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.sendAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch

abstract class ServletVerticle:CoroutineVerticle() {

  fun start(address:String) {

    vertx.eventBus().consumer<JsonObject>(address){
      val reqJson = it.body()
      val session:Session = Session(reqJson.getJsonObject(COOKIES).getString(SESSION_ID))

      launch {
        when(reqJson.getString(HTTP_METHOD)){
          POST -> {
            it.reply(doPost(reqJson, session))
          }
          GET -> {
            it.reply(doGet(reqJson, session))
          }
          else -> it.reply(JsonObject().put(JSON_BODY,"Http Method is not specified"))
        }
      }
    }
  }

  inner class Session(private val id: String) {

    fun put(key:String, value:String){
      vertx.eventBus().send(SessionVerticle::class.java.name,
        JsonObject().put(ACTION, PUT).put(SESSION_ID, id).put(INFORMATION, JsonObject().put(key, value))
      )
    }

    fun remove(key:String){
      vertx.eventBus().send(SessionVerticle::class.java.name,
        JsonObject().put(ACTION, REMOVE).put(SESSION_ID, id).put(INFORMATION, key)
      )
    }

    suspend fun get(key:String):String?{
      val result = vertx.eventBus().sendAwait<String>(SessionVerticle::class.java.name,
        JsonObject().put(ACTION, GET).put(SESSION_ID, id).put(INFORMATION, key)
      )
      return result.body()
    }
  }

  open suspend fun doGet(json:JsonObject, session:Session):JsonObject{
    return json
  }

  open suspend fun doPost(json:JsonObject, session:Session):JsonObject{
    return json
  }

}
