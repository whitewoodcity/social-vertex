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

import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.time.Duration


@RunWith(VertxUnitRunner::class)
class UtilTest{
  @Test
  fun testMd5(context: TestContext){
    context.assertEquals(md5("123456"),"e10adc3949ba59abbe56e057f20f883e")
  }

  @Test
  fun testSensitiveWords(context: TestContext){
    context.assertTrue(containsSensitiveWords("shit001"))
    context.assertFalse(containsSensitiveWords("zhaoce"))
    context.assertTrue(containsSensitiveWords("fuckyou"))
  }

  @Test
  fun testLowerCaseJsonField(context: TestContext){
    val json = JsonObject().put("field",111)
      .put("field0","ZHAOCE")
      .lowerCaseValue("field")
      .lowerCaseValue("field0")

    context.assertEquals(json.getInteger("field"), 111)
    context.assertEquals(json.getString("field0"), "zhaoce")
  }

  @Test
  fun test(context: TestContext){
    val date0 = LocalDateTime.parse("2019-01-01T00:00")
    var date1 = LocalDateTime.parse("2018-01-01T00:00")

    println(Math.abs(Duration.between(date1, date0).toDays()))

    while(!date1.isBefore(date0)){
      date1 = date1.minusDays(1)
    }

    println(date1.minusDays(365))
  }
}
