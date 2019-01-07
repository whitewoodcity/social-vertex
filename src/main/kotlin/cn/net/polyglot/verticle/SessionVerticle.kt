package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle

class SessionVerticle : CoroutineVerticle() {
  private val sessions = HashMap<String, HashMap<String, String>>()
  private val sessionUpdateTime = HashMap<String, Long>()

  override suspend fun start() {
    vertx.eventBus().consumer<JsonObject>(this::class.java.name){
      val sessionId = it.body().getString(SESSION_ID)

      if(!sessions.keys.contains(sessionId)){
        sessions[sessionId] = HashMap()
        sessionUpdateTime[sessionId] = System.currentTimeMillis()
      }

      when(it.body().getString(ACTION)){
        PUT ->{
          val info = it.body().getJsonObject(INFORMATION)
          val map = sessions[sessionId]!!
          for(key in info.map.keys){
            map[key] = info.map[key].toString()
          }
        }
        GET ->{
          val info = it.body().getString(INFORMATION)
          val result = sessions[sessionId]!!
          it.reply(result[info])
        }
        REMOVE ->{
          val info = it.body().getString(INFORMATION)
          sessions.remove(info)
          sessionUpdateTime.remove(info)
        }
      }
    }

    vertx.setPeriodic(5*60*1000){
      val toBeRemovedSet = HashSet<String>()

      for(sessionTimePair in sessionUpdateTime){
        if(System.currentTimeMillis() - sessionTimePair.value > 2*60*60*1000){
          toBeRemovedSet.add(sessionTimePair.key)
        }
      }

      sessions.keys.removeAll(toBeRemovedSet)
      sessionUpdateTime.keys.removeAll(toBeRemovedSet)
    }

    println("${this::class.java.name} is deployed")
  }
}
