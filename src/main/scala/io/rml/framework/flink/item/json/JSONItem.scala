/*
 * Copyright (c) 2017 Ghent University - imec
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.rml.framework.flink.item.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import io.rml.framework.flink.item.Item
import org.jsfr.json.provider.JacksonProvider
import org.jsfr.json.{JacksonParser, JsonSurfer}
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

class JSONItem(map: java.util.Map[String, Object]) extends Item {

  val LOG = LoggerFactory.getLogger(JSONItem.getClass)

  override def refer(reference: String): Option[String] = {
    try {

      val sanitizedReference: String = if (reference.contains(' ')) s"['$reference']" else reference
      val checkedReference = if (sanitizedReference.contains('$')) sanitizedReference else "$." + sanitizedReference

      // Some(next.toString.replaceAll("\"", "")) still necessary?
      val _object: Object = JsonPath.read(map, checkedReference)
      if (_object.isInstanceOf[String]) Some(_object.asInstanceOf[String])
      else if (_object.isInstanceOf[java.lang.Integer]) Some(_object.asInstanceOf[Integer].toString)
      else if (_object.isInstanceOf[java.lang.Long]) Some(_object.asInstanceOf[Long].toString)
      else {
        println(_object);
        None
      }

    } catch {
      case NonFatal(e) => {
        println(e)
        None
      }
    }
  }
}

object JSONItem {

  private val surfer = new JsonSurfer(JacksonParser.INSTANCE, JacksonProvider.INSTANCE)

  def fromString(json: String): JSONItem = {
    val mapper = new ObjectMapper()
    val node = mapper.readTree(json)
    null //new JSONItem(node)
  }

  def fromStringOptionableList(json: String, iterator: String): Option[Array[JSONItem]] = {
    try {
      val collection = surfer.collectAll(json, iterator)
      val listOfJson = collection.toArray()
      val mapper = new ObjectMapper()

      val result = listOfJson
        .map(node => mapper.convertValue(node, classOf[java.util.Map[String, Object]]))
        .map(map => new JSONItem(map))

      Some(result)
    } catch {
      case NonFatal(e) => e.printStackTrace(); None
    }
  }
}
