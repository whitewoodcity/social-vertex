package cn.net.polyglot.verticle

import cn.net.polyglot.config.FORM_ATTRIBUTES
import cn.net.polyglot.config.TEMPLATE_PATH
import cn.net.polyglot.config.VALUES
import io.vertx.core.json.JsonObject

class SampleVerticle : ServletVerticle() {
  override suspend fun start() {
    super.start(this::class.java.name)
  }

  override suspend fun doPost(json: JsonObject, session: Session): JsonObject {

    println(json)

    return JsonObject()
      .put(VALUES, JsonObject().put("username", json.getJsonObject(FORM_ATTRIBUTES).getString("id")))
      .put(TEMPLATE_PATH, "sample/result.html")
  }
}
