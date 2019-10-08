package cn.net.polyglot.verticle.publication

import cn.net.polyglot.config.*
import cn.net.polyglot.module.lastHour
import cn.net.polyglot.module.nextHour
import com.codahale.fastuuid.UUIDGenerator
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.file.*
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import java.io.File.separator
import java.security.SecureRandom
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class PublicationVerticle : CoroutineVerticle() {
  private val generator = UUIDGenerator(SecureRandom())

  override suspend fun start() {
    vertx.eventBus().consumer<JsonObject>(this::class.java.name) {
      launch { it.reply(article(it.body())) }
    }
  }

  private suspend fun article(json: JsonObject): JsonObject {
    return try {
      when (json.getString(SUBTYPE)) {
        QUESTION, ARTICLE, IDEA, THOUGHT, ANSWER -> post(json)
        HISTORY -> history(json)
        UPDATE -> update(json)
        RETRIEVE -> retrieve(json)
        REPLY -> reply(json)
        COMMENT -> comment(json)
        COMMENT_LIST -> commentList(json)
        LIKE -> like(json)
        DISLIKE -> dislike(json)
        COLLECT -> collect(json)
        COLLECT_LIST -> collectList(json)
        else -> json.put(PUBLICATION, false)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      json.put(PUBLICATION, false).put(INFO, e.message)
    }
  }


  //收藏  todo UT
  private suspend fun collect(json: JsonObject): JsonObject {
    //get the brief or publicationo of the article
    if(!json.containsKey(DIR)) return json.put(PUBLICATION,false).put(INFO,"Directory is required")
    val dir = json.getString(DIR)
    val communityArticlePath = "${config.getString(DIR)}$separator$COMMUNITY$separator$dir"
    val articleBrief:JsonObject
    val fs = vertx.fileSystem()
    articleBrief = when {
      fs.existsAwait("$communityArticlePath$separator${BRIEF}.json") -> fs.readFileAwait("$communityArticlePath$separator${BRIEF}.json").toJsonObject()
      fs.existsAwait("$dir$separator${PUBLICATION}.json") -> fs.readFileAwait("$communityArticlePath$separator${PUBLICATION}.json").toJsonObject()
      else -> return json.put(PUBLICATION,false).put(INFO,"Article $dir dose not exists!")
    }

    //create a .collect/ dir at the root dir for the user and create empty file to index the article
    val userDir = "${config.getString(DIR)}$separator${json.getString(ID)}"
    val userCollectDir = "$userDir$separator$_COLLECT"
    if (!fs.existsAwait(userCollectDir)) {
      fs.mkdirAwait(userCollectDir)
    }
    val collectedArticles = fs.readDirAwait(userCollectDir)
    if (collectedArticles.contains(dir)){
      //uncollect case
      fs.deleteAwait("$userCollectDir$separator$dir")
    }else{
      //collect case
      articleBrief.put(COLLECTED_TIME,System.currentTimeMillis())
      fs.createFileAwait("$userCollectDir$separator$dir")
      fs.writeFileAwait("$userCollectDir$separator$dir",articleBrief.toBuffer())
    }

    //create a collect.json at the article's dir , aiming to store the userIds/num of collection
    val collectFilePath = "$communityArticlePath$separator${COLLECT}.json"
    if (!fs.existsAwait(collectFilePath)){
      fs.createFileAwait(collectFilePath)
      val initialContent = jsonObjectOf().put(COUNT,0).put(IDS, JsonArray())
      fs.writeFileAwait(collectFilePath,initialContent.toBuffer())
    }
    val collectInfo = fs.readFileAwait(collectFilePath).toJsonObject()
    val count = collectInfo.getInteger(COUNT)
    val ids:JsonArray = collectInfo.getJsonArray(IDS)
    if (ids.contains(json.getString(ID))){
      //if a user has never collected the article,add the count and ids
      collectInfo.put(COUNT,count-1)
      collectInfo.put(IDS,ids.remove(json.getString(ID)))
    }else {
      //if the user already collected the artcle, this case means that uncollect
      collectInfo.put(COUNT, count + 1)
      collectInfo.put(IDS, ids.add(json.getString(ID)))
    }
    fs.writeFileAwait(collectFilePath,collectInfo.toBuffer())

    return jsonObjectOf().put(PUBLICATION,true).put(TYPE, PUBLICATION).put(SUBTYPE, COLLECT)
  }


  //get a list of a user's collected articles. todo UT
  private suspend fun collectList(json: JsonObject): JsonObject {
    val fs = vertx.fileSystem()
    //current userId
    val id = json.getString(ID)
    val collectPath = "${config.getString(DIR)}$separator$id$separator$_COLLECT"
    val collectedArticles = fs.readDirAwait(collectPath)
    val articles = collectedArticles.map { fs.readFileAwait("$collectPath$separator$it").toJsonObject() }.sortedBy { it.getLong(COLLECTED_TIME) }.reversed()
    //todo pageable list
    // get pageable params and return one page
    return jsonObjectOf().put(SUBTYPE, COLLECT_LIST).put(PUBLICATION,true).put(INFO,articles)
  }


  //踩 todo UT
  private suspend fun dislike(json: JsonObject): JsonObject {
    val fs = vertx.fileSystem()
    if(!json.containsKey(DIR)) return json.put(PUBLICATION,false).put(INFO,"Directory is required")
    val dir = json.getString(DIR)
    val communityArticlePath = "${config.getString(DIR)}$separator$COMMUNITY$separator$dir"
    //handle community dislike info
    if (!fs.existsAwait("$communityArticlePath$separator${DISLIKE}.json")){
      fs.createFileAwait("$communityArticlePath$separator${DISLIKE}.json")
      val initialDislike = jsonObjectOf().put(COUNT,0).put(IDS, JsonArray())
      fs.writeFileAwait("$communityArticlePath$separator${DISLIKE}.json",initialDislike.toBuffer())
    }
    val articleDislikeInfo = fs.readFileAwait("$communityArticlePath$separator${DISLIKE}.json").toJsonObject()
    val ids = articleDislikeInfo.getJsonArray(IDS)
    val count = articleDislikeInfo.getInteger(COUNT)
    if (articleDislikeInfo.getJsonArray(IDS).contains(dir)){
      ids.remove(dir)
      articleDislikeInfo.put(IDS,ids)
      articleDislikeInfo.put(COUNT,count-1)
    }else{
      ids.add(dir)
      articleDislikeInfo.put(IDS,ids)
      articleDislikeInfo.put(COUNT,count+1)
    }
    fs.writeFileAwait("$communityArticlePath$separator${DISLIKE}.json",articleDislikeInfo.toBuffer())

    //-----------------------------------------------------------
    //handle user dislike info
    val userDir = "${config.getString(DIR)}$separator${json.getString(ID)}"
    val userDislikeJsonDir = "$userDir$separator${DISLIKE}.json"
    if(!fs.existsAwait(userDislikeJsonDir)) {
      fs.createFileAwait(userDislikeJsonDir)
      fs.writeFileAwait(userDislikeJsonDir, JsonArray().toBuffer())
    }
    val dislikedArticles = fs.readFileAwait(userDislikeJsonDir).toJsonArray()
    when{
      dislikedArticles.contains(dir) -> {
        // if already disliked,cancle it
        dislikedArticles.remove(dir)
        fs.writeFileAwait(userDislikeJsonDir,dislikedArticles.toBuffer())
      }
      else->{
        //if not disliked, then dislike it
        dislikedArticles.add(dir)
        fs.writeFileAwait(userDislikeJsonDir,dislikedArticles.toBuffer())
      }
    }
    //-------------
    return jsonObjectOf().put(SUBTYPE, DISLIKE).put(PUBLICATION,true)
  }

  //点赞 todo UT
  private suspend fun like(json: JsonObject): JsonObject {
    val fs = vertx.fileSystem()
    if(!json.containsKey(DIR)) return json.put(PUBLICATION,false).put(INFO,"Directory is required")
    val dir = json.getString(DIR)
    val communityArticlePath = "${config.getString(DIR)}$separator$COMMUNITY$separator$dir"
    //handle community dislike info
    if (!fs.existsAwait("$communityArticlePath$separator${LIKE}.json")){
      fs.createFileAwait("$communityArticlePath$separator${LIKE}.json")
      val initialDislike = jsonObjectOf().put(COUNT,0).put(IDS, JsonArray())
      fs.writeFileAwait("$communityArticlePath$separator${LIKE}.json",initialDislike.toBuffer())
    }
    val articleDislikeInfo = fs.readFileAwait("$communityArticlePath$separator${LIKE}.json").toJsonObject()
    val ids = articleDislikeInfo.getJsonArray(IDS)
    val count = articleDislikeInfo.getInteger(COUNT)
    if (articleDislikeInfo.getJsonArray(IDS).contains(dir)){
      ids.remove(dir)
      articleDislikeInfo.put(IDS,ids)
      articleDislikeInfo.put(COUNT,count-1)
    }else{
      ids.add(dir)
      articleDislikeInfo.put(IDS,ids)
      articleDislikeInfo.put(COUNT,count+1)
    }
    fs.writeFileAwait("$communityArticlePath$separator${LIKE}.json",articleDislikeInfo.toBuffer())

    //-----------------------------------------------------------
    //handle user dislike info
    val userDir = "${config.getString(DIR)}$separator${json.getString(ID)}"
    val userDislikeJsonDir = "$userDir$separator${LIKE}.json"
    if(!fs.existsAwait(userDislikeJsonDir)) {
      fs.createFileAwait(userDislikeJsonDir)
      fs.writeFileAwait(userDislikeJsonDir, JsonArray().toBuffer())
    }
    val dislikedArticles = fs.readFileAwait(userDislikeJsonDir).toJsonArray()
    when{
      dislikedArticles.contains(dir) -> {
        // if already disliked,cancle it
        dislikedArticles.remove(dir)
        fs.writeFileAwait(userDislikeJsonDir,dislikedArticles.toBuffer())
      }
      else->{
        //if not liked, then like it
        dislikedArticles.add(dir)
        fs.writeFileAwait(userDislikeJsonDir,dislikedArticles.toBuffer())
      }
    }
    //-------------
    return jsonObjectOf().put(SUBTYPE, LIKE).put(PUBLICATION,true)
  }

  //获取评论列表 todo UT
  private suspend fun commentList(json: JsonObject): JsonObject {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  //评论 todo UT
  private suspend fun comment(json: JsonObject): JsonObject {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  //todo 需完善以及unit tests
  private suspend fun reply(json:JsonObject):JsonObject{
    return try{
      val dir = json.getString(DIR)

      val dirPath = "${config.getString(DIR)}$separator$COMMUNITY$separator$dir"

      if(!vertx.fileSystem().existsAwait(dirPath)){
        return jsonObjectOf().put(PUBLICATION, false).put(INFO, "$dirPath doesn't exist")
      }

      json.put(TIME_ORDER_STRING, "${System.currentTimeMillis()}")
      json.put(DEFAULT_ORDER_STRING, "${System.currentTimeMillis()}")

      val file = generator.generate()

      vertx.fileSystem().writeFileAwait("$dirPath$separator$file.reply.json", json.toBuffer())

      json
    }catch (e:Throwable){
      jsonObjectOf().put(PUBLICATION, false).put(INFO, e.message)
    }

  }

  private suspend fun post(json: JsonObject): JsonObject {

    val fs = vertx.fileSystem()

    val date = Date()
    val today = SimpleDateFormat("yyyy-MM-dd").format(date)
    val time = SimpleDateFormat("hh:mm:ss").format(date)

    json.put(DATE, today)
    json.put(TIME, time)

    val yyyy = SimpleDateFormat("yyyy").format(date)
    val mm = SimpleDateFormat("MM").format(date)
    val dd = SimpleDateFormat("dd").format(date)
    val hh = SimpleDateFormat("HH").format(date)

    val dirName = generator.generate().toString()

    val communityPath = "${config.getString(DIR)}$separator$COMMUNITY$separator$yyyy$separator$mm$separator$dd$separator$hh$separator$dirName"

    fs.mkdirsAwait(communityPath)
    fs.writeFileAwait("$communityPath${separator}publication.json", json.toBuffer())

    val briefJson = json.copy()
    if(briefJson.containsKey(CONTENT) && briefJson.getValue(CONTENT) !=null &&
      briefJson.getValue(CONTENT) is String && briefJson.getString(CONTENT).length>100){
      val briefContent = briefJson.getString(CONTENT).substring(0,100).plus("...")
      briefJson.put(CONTENT, briefContent)
      fs.writeFileAwait("$communityPath${separator}brief.json", briefJson.toBuffer())
    }

    val linkPath = "${config.getString(DIR)}$separator${json.getString(ID)}$separator$COMMUNITY$separator$yyyy$separator$mm$separator$dd$separator$hh"
    fs.mkdirsAwait(linkPath)
    fs.createFileAwait("$linkPath$separator$dirName")

    return jsonObjectOf().put(PUBLICATION, true).put(DIR, "$separator$yyyy$separator$mm$separator$dd$separator$hh$separator$dirName")
  }

  private suspend fun retrieve(json: JsonObject): JsonObject {
    if (!json.containsKey(DIR)) return json.put(PUBLICATION, false).put(INFO, "Directory is required")

    val path = "${config.getString(DIR)}$separator$COMMUNITY${json.getString(DIR)}$separator" + "publication.json"

    return try {
      vertx.fileSystem().readFileAwait(path).toJsonObject().put(PUBLICATION, true)
    } catch (e: Throwable) {
      e.printStackTrace()
      json.put(PUBLICATION, false).put(INFO, e.message)
    }
  }

  //update article
  private suspend fun update(json: JsonObject): JsonObject {
    //check arg of DIR
    if(!json.containsKey(DIR)) return json.put(PUBLICATION,false).put(INFO,"Directory is required")
    val dir = "${config.getString(DIR)}$separator$COMMUNITY${json.getString(DIR)}"
    val originalPath = "$dir${separator}publication.json"
    val newPath = "$dir${separator}publication_new.json"
    try {
      //set the subtype
      val originalArticle = vertx.fileSystem().readFileAwait(originalPath).toJsonObject()
      json.put(SUBTYPE,originalArticle.getString(SUBTYPE))

      //handle brief
      val briefJson = json.copy()
      if(briefJson.containsKey(CONTENT) && briefJson.getValue(CONTENT) !=null &&
        briefJson.getValue(CONTENT) is String && briefJson.getString(CONTENT).length>100){
        val briefContent = briefJson.getString(CONTENT).substring(0,100).plus("...")
        briefJson.put(CONTENT, briefContent)
        vertx.fileSystem().writeFileAwait("$dir${separator}brief.json", briefJson.toBuffer())
      }

      //create publication_new.json
      vertx.fileSystem().writeFileAwait(newPath, json.toBuffer())

      //remove the older file
      vertx.fileSystem().deleteAwait(originalPath)

      //rename new file to publication.json
      vertx.fileSystem().moveAwait(newPath, originalPath)
    }catch (e: Throwable){
      e.printStackTrace()
      json.put(PUBLICATION,false).put(INFO,e.message)
    }
    return jsonObjectOf().put(TYPE,json.getString(TYPE)).put(SUBTYPE,json.getString(SUBTYPE)).put(PUBLICATION,true)
  }

  private suspend fun history(json: JsonObject): JsonObject {

    if (json.getString(TIME) == null) {
      val nextHour = SimpleDateFormat("yyyy-MM-dd-HH").format(Date().nextHour())
      json.put(TIME, nextHour)
    } else try {
      SimpleDateFormat("yyyy-MM-dd-HH").parse(json.getString(TIME))
    } catch (e: ParseException) {
      json.remove(TIME)
      val nextHour = SimpleDateFormat("yyyy-MM-dd-HH").format(Date().nextHour())
      json.put(TIME, nextHour)
    }

    val time = SimpleDateFormat("yyyy-MM-dd-HH").parse(json.getString(TIME)).lastHour()

    val yyyy = SimpleDateFormat("yyyy").format(time)
    val mm = SimpleDateFormat("MM").format(time)
    val dd = SimpleDateFormat("dd").format(time)
    val hh = SimpleDateFormat("HH").format(time)

    val dir = if (json.containsKey(FROM)) {
      if (!vertx.fileSystem().existsAwait("${config.getString(DIR)}$separator${json.getString(FROM)}")) {
        return json.put(PUBLICATION, false).put(INFO, "User doesn't exist")
      }
      "${config.getString(DIR)}$separator${json.getString(FROM)}$separator$COMMUNITY"
    } else {
      "${config.getString(DIR)}$separator$COMMUNITY"
    }
    if (!vertx.fileSystem().existsAwait(dir)) {
      vertx.fileSystem().mkdirsAwait(dir)
    }

    val history = jsonArrayOf()
    var until = "$yyyy-$mm-$dd-$hh"

    val yyyys = vertx.fileSystem()
      .readDirAwait(dir, "\\d{4}")
      .map { it.substringAfterLast(separator) }
      .filter { it <= yyyy }
      .sortedDescending()

    loop@ for (year in yyyys) {
      val mms = vertx.fileSystem()
        .readDirAwait("$dir$separator$year", "\\d{2}")
        .map { it.substringAfterLast(separator) }
        .filter { year + it <= yyyy + mm }
        .sortedDescending()

      for (month in mms) {
        val dds = vertx.fileSystem()
          .readDirAwait("$dir$separator$year$separator$month", "\\d{2}")
          .map { it.substringAfterLast(separator) }
          .filter { year + month + it <= yyyy + mm + dd }
          .sortedDescending()

        for (day in dds) {
          val hhs = vertx.fileSystem()
            .readDirAwait("$dir$separator$year$separator$month$separator$day", "\\d{2}")
            .map { it.substringAfterLast(separator) }
            .filter { year + month + day + it <= yyyy + mm + dd + hh }
            .sortedDescending()

          for (hour in hhs) {
            val publicationList = vertx.fileSystem()
              .readDirAwait("$dir$separator$year$separator$month$separator$day$separator$hour")

            for (publicationPath in publicationList) {
              val props = vertx.fileSystem().propsAwait(publicationPath)
              val publicationFilePath = if (props.isDirectory) {
                "$publicationPath$separator" + "publication.json"
              } else {
                "${config.getString(DIR)}$separator$COMMUNITY$separator$year$separator$month$separator$day$separator$hour$separator${publicationPath.substringAfterLast(separator)}$separator" + "publication.json"
              }
              val briefFilePath = publicationFilePath.replace("publication.json","brief.json")
              val filePath = if(vertx.fileSystem().existsAwait(briefFilePath))
                briefFilePath
              else
                publicationFilePath

              until = "$year-$month-$day-$hour"

              val file = try {
                vertx.fileSystem().readFileAwait(filePath).toJsonObject()
              }catch (e:Throwable){
                jsonObjectOf(Pair(INFO, e.message))
              }.put(DIR, publicationFilePath.substringAfterLast(COMMUNITY).substringBeforeLast(separator))

              history.add(file)
            }

            if (history.size() >= 20) break@loop
          }
        }
      }
    }

    return json.put(PUBLICATION, true).put(HISTORY, history).put(TIME, until)
  }
}
