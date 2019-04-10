package cn.net.polyglot.verticle.user

import cn.net.polyglot.config.*
import cn.net.polyglot.module.containsSensitiveWords
import cn.net.polyglot.module.lowerCaseValue
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.file.*
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import java.io.File

class UserVerticle:CoroutineVerticle() {

  override suspend fun start() {
    vertx.eventBus().consumer<JsonObject>(this::class.java.name) {
      launch { it.reply(user(it.body())) }
    }
  }

  private suspend fun user(json: JsonObject): JsonObject {

    json.lowerCaseValue(ID)

    val subtype = json.getString(SUBTYPE)
    val result = JsonObject()
      .put(TYPE, json.getString(TYPE))
      .put(SUBTYPE, subtype)
      .put(subtype, false)

    if (!json.containsKey(ID) || !json.containsKey(PASSWORD)) {
      return result
    }
    try {
      val id = json.getString(ID)
      val password = json.getString(PASSWORD)

      //validate id
      val validId = when {
        id.length < 4 || id.length > 20 -> false
        id[0].isDigit() -> false
        else -> id.all { it.isLetterOrDigit() } && !containsSensitiveWords(id)//不包含有敏感词
      }

      if (!validId)
        return result.put(INFO, "用户名格式错误，仅允许不以数字开头的数字和字母组合，长度在4到20位之间")

      //validate password
      if (password == null || password.length != 32) {
        return result.put(INFO, "秘钥格式错误")
      }

      val dir = config.getString(DIR) + File.separator + id

      return when (subtype) {
        REGISTER -> {
          if(password != json.getString(PASSWORD2)){
            return result.put(INFO, "两次输入密码不一致")
          }
          if (vertx.fileSystem().existsAwait(dir + File.separator + "user.json")) {
            return result.put(INFO, "用户已存在")
          }
          vertx.fileSystem().mkdirsAwait(dir)
          vertx.fileSystem().createFileAwait(dir + File.separator + "user.json")
          json.removeAll { it.key in arrayOf(TYPE, SUBTYPE, PASSWORD2) }
          vertx.fileSystem().writeFileAwait(dir + File.separator + "user.json", json.toBuffer())

          result.put(subtype, true)
        }
        UPDATE -> {
          if (!vertx.fileSystem().existsAwait(dir + File.separator + "user.json")) {
            return result.put(INFO, "用户不存在")
          }
          if(password != json.getString(PASSWORD2)){
            return result.put(INFO, "两次输入密码不一致")
          }
          vertx.fileSystem().deleteAwait(dir + File.separator + "user.json")
          vertx.fileSystem().createFileAwait(dir + File.separator + "user.json")
          json.removeAll { it.key in arrayOf(TYPE, SUBTYPE, PASSWORD2) }
          vertx.fileSystem().writeFileAwait(dir + File.separator + "user.json", json.toBuffer())
          result.put(subtype, true)
        }
        PROFILE -> {
          try{
            val jsonObject = vertx.fileSystem().readFileAwait(dir + File.separator + "user.json").toJsonObject()

            val fs = vertx.fileSystem()
            val friendList = JsonArray()
            val friends = fs.readDirAwait(dir)
            for (friend in friends) {
              val friendId = friend.substringAfterLast(File.separator)
              if (fs.lpropsAwait(friend).isDirectory && !friendId.startsWith(".")) {
                try {
                  friendList.add(fs.readFileAwait("$friend${File.separator}$friendId.json").toJsonObject())
                }catch (e:Throwable){
                  val friendJson = JsonObject().put(ID, friendId)
                  friendList.add(friendJson)
                }
              }
            }

            jsonObject.put(FRIENDS, friendList)

            result.put(subtype, true)
              .put(JSON_BODY, jsonObject)
          }catch(e:Throwable){
            result.put(INFO, e.message)
          }
        }
        else -> result.put(INFO, "未指定操作子类型")
      }
    } catch (e: Exception) {
      print(e)
      return result
    }
  }

}
