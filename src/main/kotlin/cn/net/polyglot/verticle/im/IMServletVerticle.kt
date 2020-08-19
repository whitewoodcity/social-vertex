package cn.net.polyglot.verticle.im

import cn.net.polyglot.config.*
import cn.net.polyglot.verticle.friend.FriendVerticle
import cn.net.polyglot.verticle.message.MessageVerticle
import cn.net.polyglot.verticle.publication.PublicationVerticle
import cn.net.polyglot.verticle.search.SearchVerticle
import cn.net.polyglot.verticle.user.UserVerticle
import cn.net.polyglot.verticle.web.ServletVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await

class IMServletVerticle:ServletVerticle() {
  override suspend fun doPut(request: HttpServletRequest): HttpServletResponse {

    val bodyJson = request.bodyAsJson()
    val type = bodyJson.getString(TYPE)
    val subtype = bodyJson.getString(SUBTYPE)

    if(type.isNullOrBlank() || subtype.isNullOrBlank()){
      HttpServletResponse(JsonObject().put("info", "未指定操作类型或操作子类型"))
    }

    return when (type) {
      USER -> {
        when(subtype){
          LOGIN -> {
            bodyJson.put(SUBTYPE, PROFILE)
            val responseMessage = vertx.eventBus().request<JsonObject>(UserVerticle::class.java.name, bodyJson).await()
            val resultJson = responseMessage.body()
            if(resultJson.containsKey(PROFILE) && resultJson.getBoolean(PROFILE)){
              if(bodyJson.getString(PASSWORD) == resultJson.getJsonObject(JSON_BODY).getString(PASSWORD)){
                val responseJson = JsonObject().put(LOGIN, true).mergeIn(resultJson.getJsonObject(JSON_BODY))
                responseJson.remove(PASSWORD)
                HttpServletResponse(responseJson)
              }else{
                HttpServletResponse(JsonObject().put(LOGIN, false).put(INFO,"密码错误"))
              }
            }else{
              HttpServletResponse(bodyJson.put(LOGIN, false))
            }
          }
          else -> {
            val responseMessage = vertx.eventBus().request<JsonObject>(UserVerticle::class.java.name, bodyJson).await()
            HttpServletResponse(responseMessage.body())
          }
        }
      }
      SEARCH -> {
        val responseJson = vertx.eventBus().request<JsonObject>(SearchVerticle::class.java.name, bodyJson.getString(KEYWORD)?:"").await()
        HttpServletResponse(responseJson.body())
      }
      FRIEND -> {
        if(this.verifyIdAndPassword(bodyJson.getString(ID), bodyJson.remove(PASSWORD) as String)){
          vertx.eventBus().send(FriendVerticle::class.java.name, bodyJson)
        }

        HttpServletResponse()
      }
      MESSAGE -> {
        if(this.verifyIdAndPassword(bodyJson.getString(ID), bodyJson.remove(PASSWORD) as String)){
          HttpServletResponse(vertx.eventBus().request<JsonObject>(MessageVerticle::class.java.name, bodyJson).await().body())
        }else
          HttpServletResponse(bodyJson.put(MESSAGE, false))
      }
      PUBLICATION -> {
        if(this.verifyIdAndPassword(bodyJson.getString(ID), bodyJson.remove(PASSWORD) as String)){
          HttpServletResponse(vertx.eventBus().request<JsonObject>(PublicationVerticle::class.java.name, bodyJson).await().body())
        }else
          HttpServletResponse(bodyJson.put(PUBLICATION, false))
      }
      else -> HttpServletResponse()
    }
  }

  private suspend fun verifyIdAndPassword(id:String, password:String):Boolean{
    val json = JsonObject().put(TYPE, USER).put(SUBTYPE, VERIFY).put(ID, id).put(PASSWORD, password)
    val responseJson = vertx.eventBus().request<JsonObject>(UserVerticle::class.java.name, json).await().body()
    return responseJson.getBoolean(VERIFY)
  }
}
