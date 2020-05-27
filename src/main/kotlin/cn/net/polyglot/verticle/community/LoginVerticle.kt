package cn.net.polyglot.verticle.community

import cn.net.polyglot.config.*
import cn.net.polyglot.module.md5
import cn.net.polyglot.verticle.user.UserVerticle
import cn.net.polyglot.verticle.web.ServletVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.requestAwait
import io.vertx.kotlin.core.file.*
import java.io.File.separator

class LoginVerticle : ServletVerticle() {

  override suspend fun doPost(request: HttpServletRequest): HttpServletResponse {

    val reqJson = when (request.getPath()) {
      "/register" -> {
        val requestJson =
          request.getFormAttributes()
            .put(SUBTYPE, REGISTER)

        val password = md5(requestJson.getString(PASSWORD))
        val password2 = md5(requestJson.getString(PASSWORD2))

        requestJson.put(PASSWORD, password)
        requestJson.put(PASSWORD2, password2)

        val result = vertx.eventBus().requestAwait<JsonObject>(UserVerticle::class.java.name, requestJson).body()
        if (!result.containsKey(REGISTER) || !result.getBoolean(REGISTER)) {
          return HttpServletResponse(HttpServletResponseType.TEMPLATE, "register.htm")
        }

        JsonObject().put(ID, requestJson.getString(ID))
          .put(PASSWORD, password)
          .put(TYPE, USER)
          .put(SUBTYPE, PROFILE)
      }
      "/update" -> {
        if (request.session.get(ID) == null) {
          return HttpServletResponse(HttpServletResponseType.TEMPLATE, "index.html")
        }

        val requestJson =
          request.getFormAttributes()
            .put(TYPE, USER)
            .put(SUBTYPE, UPDATE)
            .put(ID, request.session.get(ID))
        //update user data
        vertx.eventBus().requestAwait<JsonObject>(UserVerticle::class.java.name, requestJson).body()
        //update profile image
        val uploadFile = request.getUploadFiles().getString("portrait")
        if (!uploadFile.isNullOrBlank()) {

          val jarDir = config.getString(JAR_DIR)
          val dir = config.getString(DIR)

          val file = vertx.fileSystem().propsAwait(jarDir + separator + uploadFile)
          if (file.size() == 0L)
            vertx.fileSystem().deleteAwait(jarDir + separator + uploadFile)
          else
            vertx.fileSystem().moveAwait(jarDir + separator + uploadFile,
              dir + separator + request.session.get(ID) + separator + "portrait", copyOptionsOf().setReplaceExisting(true))
        }

        JsonObject().put(ID, request.session.get(ID))
          .put(PASSWORD, request.session.get(PASSWORD))
          .put(TYPE, USER)
          .put(SUBTYPE, PROFILE)
      }
      else -> {//default is login
        val id = request.getFormAttributes().getString(ID)
        val password = md5(request.getFormAttributes().getString(PASSWORD))

        JsonObject().put(ID, id)
          .put(PASSWORD, password)
          .put(TYPE, USER)
          .put(SUBTYPE, PROFILE)
      }
    }

    return profile(reqJson, request.session)
  }

  override suspend fun doGet(request: HttpServletRequest): HttpServletResponse {

    val session = request.session

    if (session.get(ID) == null) {
      return HttpServletResponse(HttpServletResponseType.TEMPLATE, "index.html")
    }

    val id = session.get(ID)
    val password = session.get(PASSWORD)

    return when (request.getPath()) {
      "/update" -> {
        profile(JsonObject().put(ID, id)
          .put(PASSWORD, password)
          .put(TYPE, USER)
          .put(SUBTYPE, PROFILE), session, "update.htm")
      }
      "/portrait" -> {
        val dir = config.getString(DIR)
        HttpServletResponse(dir + separator + session.get(ID) + separator + "portrait")
      }
      else -> {
        profile(JsonObject().put(ID, id)
          .put(PASSWORD, password)
          .put(TYPE, USER)
          .put(SUBTYPE, PROFILE), session)
      }
    }
  }

  private suspend fun profile(reqJson: JsonObject, session: HttpSession, defaultTemplatePath: String = "index.html"): HttpServletResponse {
    return try {
      val asyncResult = vertx.eventBus().requestAwait<JsonObject>(UserVerticle::class.java.name, reqJson).body()

      if (asyncResult.containsKey(PROFILE) && asyncResult.getBoolean(PROFILE)) {

        session.put(ID, reqJson.getString(ID))
        session.put(PASSWORD, reqJson.getString(PASSWORD))
        session.put(NICKNAME, asyncResult.getJsonObject(JSON_BODY).getString(NICKNAME))

        HttpServletResponse(defaultTemplatePath, asyncResult.getJsonObject(JSON_BODY))
      } else {
        HttpServletResponse(HttpServletResponseType.TEMPLATE, "index.html")
      }
    } catch (e: Throwable) {
      e.printStackTrace()
      HttpServletResponse(HttpServletResponseType.TEMPLATE, "error.html")
    }
  }
}
