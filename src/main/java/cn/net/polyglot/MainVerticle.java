package cn.net.polyglot;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.nio.file.Paths;

public class MainVerticle extends AbstractVerticle {
  public void start(){

    var currentPath = Paths.get("").toAbsolutePath().toString();

    System.out.println("Current Path: "+currentPath);

    JsonObject config = new JsonObject()
      .put("version",0.7)
      .put("dir", currentPath + File.separator + "social-vertex")
      .put("jar-dir", currentPath)
      .put("host", "localhost")
      .put("tcp-port", 7373)
      .put("http-port", 80)
      .put("https-port",443);

    ConfigStoreOptions fileStore = new ConfigStoreOptions()
      .setType("file")
      .setConfig(new JsonObject().put("path", "config.json"));

    ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore);

    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

    retriever.getConfig( ar -> {

      try {

        if (ar.succeeded()) {
          config.mergeIn(ar.result());
        } else {
          System.out.println("The configuration file: config.json does not exist or in wrong format, use default config.");
        }

        retriever.close();

        if (!vertx.fileSystem().existsBlocking(config.getString("dir"))) {
          vertx.fileSystem().mkdirBlocking(config.getString("dir"));
        }

        var option = new DeploymentOptions().setConfig(config);
        vertx.deployVerticle("kt:cn.net.polyglot.verticle.message.MessageVerticle", option);
        vertx.deployVerticle("kt:cn.net.polyglot.verticle.friend.FriendVerticle", option);
        vertx.deployVerticle("kt:cn.net.polyglot.verticle.search.SearchVerticle", option);
        vertx.deployVerticle("kt:cn.net.polyglot.verticle.user.UserVerticle", option);
        vertx.deployVerticle("kt:cn.net.polyglot.verticle.publication.PublicationVerticle", option);
        vertx.deployVerticle("kt:cn.net.polyglot.verticle.im.IMTcpServerVerticle", option);
        vertx.deployVerticle("kt:cn.net.polyglot.verticle.im.IMServletVerticle", option);
        vertx.deployVerticle("kt:cn.net.polyglot.verticle.WebServerVerticle", option);
        vertx.deployVerticle("kt:cn.net.polyglot.verticle.community.DefaultVerticle", option);
        vertx.deployVerticle("kt:cn.net.polyglot.verticle.community.LoginVerticle", option);
        vertx.deployVerticle("kt:cn.net.polyglot.verticle.community.CommunityVerticle", option);

      } catch (Exception e) {
        e.printStackTrace();
      }

    });

  }
}
