package cn.net.polyglot.verticle

import cn.net.polyglot.config.TypeConstants.FRIEND
import cn.net.polyglot.config.TypeConstants.MESSAGE
import cn.net.polyglot.config.TypeConstants.SEARCH
import cn.net.polyglot.config.TypeConstants.USER
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import java.io.File

class IMMessageVerticle : AbstractVerticle() {

  override fun start() {
    vertx.eventBus().consumer<JsonObject>(IMMessageVerticle::class.java.name) {
      val json = it.body()
      try{
        val type = json.getString("type")
        val action = json.getString("action")

        if(type==null){
          it.reply(JsonObject().putNull("type"))
          return@consumer
        }

        if(action==null) {
          it.reply(JsonObject().putNull("action"))
          return@consumer
        }
      }catch (e:Exception){
        it.reply(JsonObject()
          .putNull("type").putNull("action")
          .put("info",e.message))
        return@consumer
      }

      launch(vertx.dispatcher()) {
        when (it.body().getString("type")) {
          USER -> it.reply(user(json))
          SEARCH -> search(json)
          FRIEND -> friend(json)
          MESSAGE -> message(json)
          else -> {
          }
        }
      }
    }
  }

  private fun user(json: JsonObject) :JsonObject{
    val action = json.getString("action")
    val result = JsonObject().put(action, false)

    if(!json.containsKey("user")||!json.containsKey("crypto")){
      return result
    }
    try{
      val user = json.getString("user")
      val crypto = json.getString("crypto")

      //validate user
      val validUser = when {
        user.length < 4 || user.length > 20 -> false
        user[0].isDigit() -> false
        else -> user.all { it.isLetterOrDigit() }
      }

      if(!validUser)
        return result.put("info","用户名格式错误，仅允许不以数字开头的数字和字母组合，长度在4到20位之间")

      //validate crypto
      if(crypto == null || crypto.length != 32){
        return result.put("info","秘钥格式错误")
      }

      val dir = config().getString("dir") + File.separator + user

      when(action){
        "register" -> {
          if(vertx.fileSystem().existsBlocking(dir + File.separator + "user.json")){
            return result.put("info","用户已存在")
          }
          vertx.fileSystem().mkdirsBlocking(dir)
          vertx.fileSystem().createFileBlocking(dir + File.separator + "user.json")
          vertx.fileSystem().writeFileBlocking(dir + File.separator + "user.json", json.toBuffer())

          return result.put(action,true)
        }
        else -> {//login as default action
          if(!vertx.fileSystem().existsBlocking(dir + File.separator + "user.json")){
            return result.put(action, false)
          }
          val userJson = vertx.fileSystem().readFileBlocking(dir + File.separator + "user.json").toJsonObject()
          return result.put(action, userJson.getString("crypto") == json.getString("crypto"))
        }
      }
    }catch (e:Exception){
      return result.put("info",e.message)
    }
  }

  private fun search(json: JsonObject) {

  }

  private fun friend(json: JsonObject) {

  }

  private fun message(json: JsonObject) {

  }
}
