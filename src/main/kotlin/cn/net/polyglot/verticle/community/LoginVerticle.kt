package cn.net.polyglot.verticle.community

import cn.net.polyglot.config.*
import cn.net.polyglot.module.md5
import cn.net.polyglot.verticle.user.UserVerticle
import cn.net.polyglot.verticle.web.ServletVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.sendAwait
import io.vertx.kotlin.core.file.readFileAwait
import java.io.File

class LoginVerticle : ServletVerticle() {

  override suspend fun doPost(json: JsonObject, session: Session): JsonObject {

    val reqJson = when(json.getString(PATH)){
      "/register" -> {
        val requestJson =
          json.getJsonObject(FORM_ATTRIBUTES)
            .put(SUBTYPE, REGISTER)

        val password = md5(requestJson.getString(PASSWORD))
        val password2 = md5(requestJson.getString(PASSWORD2))

        requestJson.put(PASSWORD, password)
        requestJson.put(PASSWORD2, password2)

        val result = vertx.eventBus().sendAwait<JsonObject>(UserVerticle::class.java.name,requestJson).body()
        if(!result.containsKey(REGISTER) || !result.getBoolean(REGISTER)){
          return JsonObject().put(VALUES, result).put(TEMPLATE_PATH, "register.html")
        }

        JsonObject().put(ID, requestJson.getString(ID))
          .put(PASSWORD, password)
          .put(TYPE, USER)
          .put(SUBTYPE, PROFILE)
      }
      "/update" -> {
        if (session.get(ID) == null) {
          return JsonObject()
            .put(TEMPLATE_PATH, "index.htm")
        }

        JsonObject().put(ID, session.get(ID))
          .put(PASSWORD, session.get(PASSWORD))
          .put(TYPE, USER)
          .put(SUBTYPE, PROFILE)
      }
      else -> {//default is login
        val id = json.getJsonObject(FORM_ATTRIBUTES).getString(ID)
        val password = md5(json.getJsonObject(FORM_ATTRIBUTES).getString(PASSWORD))

        JsonObject().put(ID, id)
          .put(PASSWORD, password)
          .put(TYPE, USER)
          .put(SUBTYPE, PROFILE)
      }
    }

    return profile(reqJson, session)
  }

  override suspend fun doGet(json: JsonObject, session: Session): JsonObject {
    if (session.get(ID) == null) {
      return JsonObject()
        .put(TEMPLATE_PATH, "index.htm")
    }

    val id = session.get(ID)
    val password = session.get(PASSWORD)

    val reqJson =
      JsonObject().put(ID, id)
        .put(PASSWORD, password)
        .put(TYPE, USER)
        .put(SUBTYPE, PROFILE)

    return profile(reqJson, session)
  }

  private suspend fun profile(reqJson: JsonObject, session: Session):JsonObject{
    return try{
      val asyncResult = vertx.eventBus().sendAwait<JsonObject>(UserVerticle::class.java.name, reqJson).body()

      if(asyncResult.containsKey(PROFILE) && asyncResult.getBoolean(PROFILE)){

        session.put(ID, reqJson.getString(ID))
        session.put(PASSWORD, reqJson.getString(PASSWORD))
        session.put(NICKNAME, asyncResult.getJsonObject(JSON_BODY).getString(NICKNAME))

        JsonObject()
          .put(VALUES,asyncResult.getJsonObject(JSON_BODY))
          .put(TEMPLATE_PATH, "index.html")
      }else{
        JsonObject()
          .put(TEMPLATE_PATH, "index.htm")
      }
    }catch (e:Throwable){
      JsonObject()
        .put(TEMPLATE_PATH, "error.htm")
    }
  }
}
