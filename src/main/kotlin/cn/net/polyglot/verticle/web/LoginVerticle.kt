package cn.net.polyglot.verticle.web

import cn.net.polyglot.config.*
import cn.net.polyglot.module.md5
import cn.net.polyglot.verticle.im.IMMessageVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.sendAwait
import io.vertx.kotlin.core.file.readFileAwait
import java.io.File

class LoginVerticle : ServletVerticle() {
  override suspend fun start() {
    super.start(this::class.java.name)
  }

  override suspend fun doPost(json: JsonObject, session: Session): JsonObject {

    val id = json.getJsonObject(FORM_ATTRIBUTES).getString(ID)
    val password = md5(json.getJsonObject(FORM_ATTRIBUTES).getString(PASSWORD))

    val reqJson =
      JsonObject().put(ID, id)
        .put(PASSWORD, password)
        .put(TYPE, USER)
        .put(SUBTYPE, LOGIN)

    return profile(reqJson, session)
  }

  override suspend fun doGet(json: JsonObject, session: Session): JsonObject {
    if (session.get(ID) == null) {
      return JsonObject()
        .put(TEMPLATE_PATH, "index.htm")
    }

    val dir = config.getString(DIR)
    val path = dir + File.separator + session.get(ID) + File.separator + "user.json"
    val id = session.get(ID)
    val password = vertx.fileSystem().readFileAwait(path).toJsonObject().getString(PASSWORD)

    val reqJson =
      JsonObject().put(ID, id)
        .put(PASSWORD, password)
        .put(TYPE, USER)
        .put(SUBTYPE, LOGIN)

    return profile(reqJson, session)
  }

  private suspend fun profile(reqJson: JsonObject, session: Session):JsonObject{
    return try{
      val asyncResult = vertx.eventBus().sendAwait<JsonObject>(IMMessageVerticle::class.java.name, reqJson).body()

      if(asyncResult.containsKey(LOGIN) && asyncResult.getBoolean(LOGIN)){

        session.put(ID, reqJson.getString(ID))
        session.put(NICKNAME, asyncResult.getString(NICKNAME))

        JsonObject()
          .put(VALUES,asyncResult)
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
