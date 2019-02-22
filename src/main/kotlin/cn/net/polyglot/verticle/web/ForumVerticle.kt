package cn.net.polyglot.verticle.web

import cn.net.polyglot.config.*
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.file.existsAwait
import java.io.File
import com.codahale.fastuuid.UUIDGenerator
import io.vertx.core.file.OpenOptions
import io.vertx.kotlin.core.file.createFileAwait
import io.vertx.kotlin.core.file.mkdirsAwait
import io.vertx.kotlin.core.file.openAwait
import java.security.SecureRandom
import java.time.LocalDateTime

class ForumVerticle : ServletVerticle() {
  private val generator = UUIDGenerator(SecureRandom())

  override suspend fun start() {
    super.start(this::class.java.name)
  }

  override suspend fun doPost(json: JsonObject, session: Session): JsonObject {

    return try{
      if(session.get(ID) == null){
        return JsonObject()
          .put(TEMPLATE_PATH, "index.htm")
      }

      val dir = config.getString(DIR)

      val now = LocalDateTime.now()

      val datePath = dir+ File.separator+ COMMUNITY + File.separator + now.year + File.separator + now.monthValue + File.separator + now.dayOfMonth

      if(!vertx.fileSystem().existsAwait(datePath)){
        vertx.fileSystem().mkdirsAwait(datePath)
      }

      val fullPath = datePath+File.separator+generator.generate().toString() + ".json"

      vertx.fileSystem().createFileAwait(fullPath)
      vertx.fileSystem().openAwait(fullPath, OpenOptions().setAppend(true))
        .write(json.getJsonObject(FORM_ATTRIBUTES)
          .put(ID, session.get(ID))
          .put(NICKNAME, session.get(NICKNAME)).toBuffer())

      JsonObject().put(TEMPLATE_PATH, "dontuknow/index.html")
    }catch (throwable:Throwable){
      JsonObject().put(TEMPLATE_PATH, "error.html")
    }
  }
}
