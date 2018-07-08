package cn.net.polyglot

import cn.net.polyglot.config.ConfigLoaderKt
import io.vertx.config.ConfigRetriever
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

Vertx vertx = vertx

// load config.json
options = ConfigLoaderKt.options

ConfigRetriever retriever = ConfigRetriever.create(vertx, options)

retriever.getConfig { ar ->

  if (ar.failed()) {
    failedDefault()
  } else {

    JsonObject config = ar.result()
    if (ConfigLoaderKt.checkPortValidFromConfig(config)) {

      deployVerticles(config)

    } else {
      failedDefault()
    }
  }
}

private void deployVerticles(JsonObject config) {

  vertx.deployVerticle(SecondVerticle.class.name, new DeploymentOptions().setConfig(config))

  ConfigLoaderKt.portInc(config)

  vertx.deployVerticle(CoroutineVerticle.class.name, new DeploymentOptions().setConfig(config))
}


private void failedDefault() {

  System.out.println("The configuration file: config.json does not exist or in wrong format, use default config.")

  JsonObject config = ConfigLoaderKt.defaultJsonObject

  deployVerticles(config)
}
