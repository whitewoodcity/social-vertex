/**
MIT License

Copyright (c) 2018 White Wood City

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package cn.net.polyglot.module

import cn.net.polyglot.config.CONTENT
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.core.json.jsonObjectOf
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.*


@RunWith(VertxUnitRunner::class)
class UtilTest{
  @Test
  fun `test md5`(context: TestContext){
    context.assertEquals(md5("123456"),"e10adc3949ba59abbe56e057f20f883e")
  }

  @Test
  fun `test sensitive words`(context: TestContext){
    context.assertTrue(containsSensitiveWords("shit001"))
    context.assertFalse(containsSensitiveWords("zhaoce"))
    context.assertTrue(containsSensitiveWords("fuckyou"))
  }

  @Test
  fun `test lowercase json field`(context: TestContext){
    val json = JsonObject().put("field",111)
      .put("field0","ZHAOCE")
      .lowerCaseValue("field")
      .lowerCaseValue("field0")

    context.assertEquals(json.getString("field"), "111")
    context.assertEquals(json.getString("field0"), "zhaoce")
  }

  @Test
  fun `test tomorrow and yesterday`(context: TestContext){
    val simpleDateFormat = SimpleDateFormat("yyyy MM dd")
    println(Date().tomorrow())
    println(Date().yesterday())
    println(Date().inNextYear())
    context.assertEquals(simpleDateFormat.format(Date()),
      simpleDateFormat.format(Date().tomorrow().yesterday()))
  }


  @Test
  fun `test alphabetical order`(context: TestContext){
    val list = ArrayList<String>()
    list.add("1562232525005")
    list.add("1562232525005_1562232525007")
    list.add("1562232525006")
    list.sortBy{it}
    println(list)
    println(System.currentTimeMillis())
  }

  @Test
  fun `test regular expression for latin latter & digits`(context: TestContext){
    println("0123456789abcdefghijlkmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".matches(Regex("[a-zA-Z0-9]+")))

    val json = jsonObjectOf(Pair(CONTENT,"test"))
    println(json.get<Any>(CONTENT))

    val briefJson = json.copy()
    println(briefJson.containsKey(CONTENT) && briefJson.getValue(CONTENT) !=null &&
      briefJson.getValue(CONTENT) is String)
//    if(briefJson.containsKey(CONTENT) && briefJson.get<Any>(CONTENT) != null &&
//      briefJson.get<Any>(CONTENT) is String){
//      println(true)
//    }else{
//      println(false)
//    }

  }

}
