package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.file.existsAwait
import io.vertx.kotlin.core.file.readFileAwait
import java.io.File

class LoginVerticle : ServletVerticle() {
  override suspend fun start() {
    super.start(this::class.java.name)
  }

  override suspend fun doPost(json: JsonObject, session: Session): JsonObject {

    val id = json.getJsonObject(FORM_ATTRIBUTES).getString(ID)
    val password = json.getJsonObject(FORM_ATTRIBUTES).getString(PASSWORD)

    var verified = false
    if(vertx.fileSystem().existsAwait(config.getString(DIR) + File.separator + id+ File.separator + "user.json")){
      val fileJson = vertx.fileSystem().readFileAwait(config.getString(DIR) + File.separator + id+ File.separator + "user.json").toJsonObject()
      if(fileJson.getString(PASSWORD) == password)
        verified = true
    }

    return if(verified){
      JsonObject()
        .put(VALUES, JsonObject().put("username", json.getJsonObject(FORM_ATTRIBUTES).getString("id")))
        .put(TEMPLATE_PATH, "sample/result.html")
    }else{
      JsonObject()
        .put(TEMPLATE_PATH, "index.htm")
    }


  }
}
