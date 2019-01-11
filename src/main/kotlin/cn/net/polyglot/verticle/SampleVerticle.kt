package cn.net.polyglot.verticle

import io.vertx.core.json.JsonObject

class SampleVerticle : ServletVerticle() {
  override suspend fun start() {
    super.start(this::class.java.name)
  }

  override suspend fun doGet(json: JsonObject, session: Session): JsonObject {
    return super.doGet(json, session)
  }
}
