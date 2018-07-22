package cn.net.polyglot

import cn.net.polyglot.verticle.IMHttpServerVerticle
import cn.net.polyglot.verticle.IMTcpServerVerticle
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Launcher
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

Vertx vertx = vertx

JsonObject tcpVerticleConfig = new JsonObject().put("port", 7373)

JsonObject httpVerticleConfig = new JsonObject().put("port", 7575)

JsonObject config = new JsonObject()
  .put("dir", new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() + File.separator + "data")

config.put("tcp-verticle", tcpVerticleConfig)
  .put("http-verticle", httpVerticleConfig)

ConfigStoreOptions fileStore = new ConfigStoreOptions()
  .setType("file")
  .setConfig(new JsonObject().put("path", "config.json"))

ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore)

ConfigRetriever retriever = ConfigRetriever.create(vertx, options)

retriever.getConfig { ar ->

  try {

    if (ar.succeeded()) {
      config.mergeIn(JsonObject.mapFrom(ar.result()))
    } else {
      System.out.println("The configuration file: config.json does not exist or in wrong format, use default config.")
    }

    retriever.close()

    if (!vertx.fileSystem().existsBlocking(config.getString("dir"))) {
      vertx.fileSystem().mkdirBlocking(config.getString("dir"))
    }

    vertx.deployVerticle(IMHttpServerVerticle.class.name, new DeploymentOptions().setConfig(config.getJsonObject("http-verticle")))
    vertx.deployVerticle(IMTcpServerVerticle.class.name, new DeploymentOptions().setConfig(config.getJsonObject("tcp-verticle")))

  } catch (Exception e) {
    e.printStackTrace()
  }

}
