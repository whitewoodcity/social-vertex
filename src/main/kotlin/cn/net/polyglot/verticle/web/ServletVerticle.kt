package cn.net.polyglot.verticle.web

import cn.net.polyglot.config.*
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.sendAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch

abstract class ServletVerticle:CoroutineVerticle() {

  override suspend fun start(){
    start(this::class.java.asSubclass(this::class.java).name)//缺省以实际继承类名作为地址
  }

  protected fun start(address:String) {
    vertx.eventBus().consumer<JsonObject>(address){
      val reqJson = it.body()
      val session = Session(reqJson.getJsonObject(COOKIES).getString(SESSION_ID))

      launch {
        when(reqJson.getString(HTTP_METHOD)){
          POST -> {
            it.reply(doPost(reqJson, session))
          }
          GET -> {
            it.reply(doGet(reqJson, session))
          }
          PUT -> {
            it.reply(doPut(reqJson, session))
          }
          else -> it.reply(JsonObject().put(JSON_BODY,"Http Method is not specified"))
        }
      }
    }
  }

  //Service Proxy of the Session Verticle
  inner class Session(private val id: String) {

    fun put(key:String, value:String?){
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

  open suspend fun doGet(json:JsonObject, session: Session):JsonObject{
    return JsonObject().put(EMPTY_RESPONSE, true)
  }

  open suspend fun doPost(json:JsonObject, session: Session):JsonObject{
    return JsonObject().put(EMPTY_RESPONSE, true)
  }

  open suspend fun doPut(json:JsonObject, session: Session):JsonObject{
    return JsonObject().put(EMPTY_RESPONSE, true)
  }
}
