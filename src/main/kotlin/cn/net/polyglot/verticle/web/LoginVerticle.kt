package cn.net.polyglot.verticle.web

import cn.net.polyglot.config.*
import cn.net.polyglot.module.md5
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.file.existsAwait
import io.vertx.kotlin.core.file.readDirAwait
import io.vertx.kotlin.core.file.readFileAwait
import java.io.File
import java.io.File.separator

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
      if(fileJson.getString(PASSWORD) == md5(password))
        verified = true
    }

    return if(verified){
      session.put(ID, id)
      JsonObject().put(VALUES,
          JsonObject()
            .put("username", id)
            .put("friends", retrieveFriends(id))
        )
        .put(TEMPLATE_PATH, "sample/result.html")
    }else{
      JsonObject()
        .put(TEMPLATE_PATH, "index.htm")
    }
  }

  private suspend fun retrieveFriends(id:String):JsonArray{
    val friends = JsonArray()

    val dir = config.getString(DIR)

    if(vertx.fileSystem().existsAwait("$dir$separator$id")){
      val list = vertx.fileSystem().readDirAwait("$dir$separator$id")
      for(string in list){
        if(!string.contains("."))
          friends.add(string)
      }
    }

    return friends
  }
}
