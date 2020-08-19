package cn.net.polyglot.verticle.web

import cn.net.polyglot.config.*
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.launch

abstract class ServletVerticle : CoroutineVerticle() {

  override suspend fun start() {
    start(this::class.java.asSubclass(this::class.java).name)//缺省以实际继承类名作为地址
    println("${this::class.java.asSubclass(this::class.java).name} is deployed")
  }

  protected fun start(address: String) {
    vertx.eventBus().consumer<JsonObject>(address) {
      val reqJson = it.body()
      val session = HttpSession(reqJson.getJsonObject(COOKIES).getString(SESSION_ID))

      launch {
        val request = HttpServletRequest(reqJson, session)
        when (reqJson.getString(HTTP_METHOD)) {
          POST -> it.reply(doPost(request).toJson())
          GET -> it.reply(doGet(request).toJson())
          PUT -> it.reply(doPut(request).toJson())
          else -> it.reply(JsonObject().put(JSON_BODY, "Http Method is not specified"))
        }
      }
    }
  }

  //Service Proxy of the Session Verticle
  inner class HttpSession(private val id: String) {

    fun put(key: String, value: String?) {
      vertx.eventBus().send(SessionVerticle::class.java.name,
        JsonObject().put(ACTION, PUT).put(SESSION_ID, id).put(INFORMATION, JsonObject().put(key, value))
      )
    }

    fun remove(key: String) {
      vertx.eventBus().send(SessionVerticle::class.java.name,
        JsonObject().put(ACTION, REMOVE).put(SESSION_ID, id).put(INFORMATION, key)
      )
    }

    suspend fun get(key: String): String? {
      val result = vertx.eventBus().request<String>(SessionVerticle::class.java.name,
        JsonObject().put(ACTION, GET).put(SESSION_ID, id).put(INFORMATION, key)
      ).await()
      return result.body()
    }
  }

  enum class HttpServletResponseType {
    EMPTY_RESPONSE, TEMPLATE, FILE, JSON
  }

  inner class HttpServletResponse(val type: HttpServletResponseType, val path: String = "index.htm",
                                  private val json: JsonObject = JsonObject(),private val jsonString: String = json.toString()) {
    constructor(json: JsonObject) : this(HttpServletResponseType.JSON, json = json)
    constructor(json: JsonArray) : this(HttpServletResponseType.JSON, jsonString = json.toString())
    constructor() : this(HttpServletResponseType.EMPTY_RESPONSE)
    constructor(filePath: String) : this(HttpServletResponseType.FILE, filePath)
    constructor(templatePath: String, json: JsonObject) : this(HttpServletResponseType.TEMPLATE, templatePath, json)

    fun toJson(): JsonObject {
      return when (type) {
        HttpServletResponseType.TEMPLATE -> JsonObject().put(TEMPLATE_PATH, path).put(VALUES, json)
        HttpServletResponseType.FILE -> JsonObject().put(FILE_PATH, path)
        HttpServletResponseType.JSON -> JsonObject().put(RESPONSE_JSON, jsonString)
        else -> JsonObject().put(EMPTY_RESPONSE, true)
      }
    }
  }

  inner class HttpServletRequest(val json:JsonObject, val session: HttpSession){
    fun getResponse():HttpServletResponse{
      return HttpServletResponse()
    }
    fun getResponse(json: JsonObject):HttpServletResponse{
      return HttpServletResponse(json)
    }
    fun getResponse(json: JsonArray):HttpServletResponse{
      return HttpServletResponse(json)
    }
    fun getResponse(filePath: String):HttpServletResponse{
      return HttpServletResponse(filePath)
    }
    fun getResponse(templatePath: String, values: JsonObject):HttpServletResponse{
      return HttpServletResponse(templatePath, values)
    }

    fun getPath():String{
      return json.getString(PATH)
    }

    fun getHttpMethod():String{
      return json.getString(HTTP_METHOD)
    }

    fun getCookies():JsonObject{
      return json.getJsonObject(COOKIES)
    }

    fun getHeaders():JsonObject{
      return json.getJsonObject(HEADERS)
    }

    fun getParams():JsonObject{
      return json.getJsonObject(PARAMS)
    }

    fun getQueryParams():JsonObject{
      return json.getJsonObject(QUERY_PARAM)
    }

    fun getFormAttributes():JsonObject{
      return json.getJsonObject(FORM_ATTRIBUTES)
    }

    fun getUploadFiles():JsonObject{
      return json.getJsonObject(UPLOAD_FILES)
    }

    fun getUploadFileNames():JsonObject{
      return json.getJsonObject(UPLOAD_FILE_NAMES)
    }

    fun bodyAsJson():JsonObject{
      return json.getJsonObject(BODY_AS_JSON)
    }
  }

  open suspend fun doGet(request:HttpServletRequest): HttpServletResponse {
    return request.getResponse()
  }

  open suspend fun doPost(request:HttpServletRequest): HttpServletResponse {
    return request.getResponse()
  }

  open suspend fun doPut(request:HttpServletRequest): HttpServletResponse {
    return request.getResponse()
  }
}
