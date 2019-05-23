package cn.net.polyglot.verticle.im

import cn.net.polyglot.config.*
import cn.net.polyglot.verticle.friend.FriendVerticle
import cn.net.polyglot.verticle.search.SearchVerticle
import cn.net.polyglot.verticle.user.UserVerticle
import cn.net.polyglot.verticle.web.ServletVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.sendAwait

class IMServletVerticle:ServletVerticle() {
  override suspend fun doPut(json: JsonObject, session: ServletVerticle.Session): Response {

    val bodyJson = json.getJsonObject(BODY_AS_JSON)
    val type = bodyJson.getString(TYPE)
    val subtype = bodyJson.getString(SUBTYPE)

    if(type.isNullOrBlank() || subtype.isNullOrBlank()){
      Response(JsonObject().put("info", "未指定操作类型或操作子类型"))
    }

    return when (type) {
      USER -> {
        when(subtype){
          LOGIN -> {
            bodyJson.put(SUBTYPE, PROFILE)
            val responseMessage = vertx.eventBus().sendAwait<JsonObject>(UserVerticle::class.java.name, bodyJson)
            val resultJson = responseMessage.body()
            if(resultJson.containsKey(PROFILE) && resultJson.getBoolean(PROFILE)){
              if(bodyJson.getString(PASSWORD) == resultJson.getJsonObject(JSON_BODY).getString(PASSWORD)){
                val responseJson = JsonObject().put(LOGIN, true).mergeIn(resultJson.getJsonObject(JSON_BODY))
                responseJson.remove(PASSWORD)
                Response(responseJson)
              }else{
                Response(JsonObject().put(LOGIN, false).put(INFO,"密码错误"))
              }
            }else{
              Response(bodyJson.put(LOGIN, false))
            }
          }
          else -> {
            val responseMessage = vertx.eventBus().sendAwait<JsonObject>(UserVerticle::class.java.name, bodyJson)
            Response(responseMessage.body())
          }
        }
      }
      SEARCH -> {
        val responseJson = vertx.eventBus().sendAwait<JsonObject>(SearchVerticle::class.java.name, bodyJson.getString(KEYWORD)?:"")
        Response(responseJson.body())
      }
      FRIEND -> {
        if(this.verifyIdAndPassword(bodyJson.getString(ID), bodyJson.getString(PASSWORD))){
          vertx.eventBus().send(FriendVerticle::class.java.name, bodyJson)
        }

        Response()
      }
      MESSAGE -> {
        vertx.eventBus().send(IMMessageVerticle::class.java.name, bodyJson)
        Response()
      }
      else ->{
        val responseJson = vertx.eventBus().sendAwait<JsonObject>(IMMessageVerticle::class.java.name, bodyJson).body()
        Response(responseJson)
      }
    }
  }

  suspend fun verifyIdAndPassword(id:String, password:String):Boolean{
    val json = JsonObject().put(TYPE, USER).put(SUBTYPE, VERIFY).put(ID, id).put(PASSWORD, password)
    val responseJson = vertx.eventBus().sendAwait<JsonObject>(UserVerticle::class.java.name, json).body()
    return responseJson.getBoolean(VERIFY)
  }
}
