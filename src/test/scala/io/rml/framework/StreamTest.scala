package io.rml.framework

import java.io.File
import java.util.concurrent.Executors

import io.rml.framework.util.fileprocessing.{DataSourceTestUtil, ExpectedOutputTestUtil}
import io.rml.framework.util.{Logger, Sanitizer, TCPUtil, TestSink}
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.scalatest.{FunSuite, Matchers}

class StreamTest extends FunSuite with Matchers {

  test("TCPSource - pull") {
    pending
    implicit val env = ExecutionEnvironment.getExecutionEnvironment
    implicit val senv = StreamExecutionEnvironment.getExecutionEnvironment

    val pool = Executors.newCachedThreadPool()

    // read the mapping
    val formattedMapping = TCPUtil.readMapping("stream/mapping.ttl")

    // execute
    val dataStream = Main.createStreamFromFormattedMapping(formattedMapping).addSink(TestSink())
    //TODO write to collection for assertions
    //    var messages = List("{\n  \"students\": [{\n    \"ID\": 10,\n    \"FirstName\":\"Venus\",\n    \"LastName\":\"Williams\"\n  },\n    {\n      \"ID\": 20,\n      \"FirstName\":\"Minerva\",\n      \"LastName\":\"Tenebare\"\n    }\n  ]\n}")
    //    messages = Sanitizer.sanitize(messages)
    //    messages = List(messages.head.replaceAll("\n","") + "\n\r")
    val messages = DataSourceTestUtil.processFile(new File("/home/sitt/Documents/idlab/rml-streamer/src/test/resources/stream/datasource.json"))
    Logger.logInfo(messages.toString())

    val server = new Runnable {
      override def run(): Unit = {
        TCPUtil.createTCPServer(9999, messages.iterator)
      }
    }

    val job = new Runnable {
      override def run(): Unit = senv.execute()
    }

    pool.submit(server)
    Thread.sleep(2000)
    pool.submit(job)
    Thread.sleep(5000)
    StreamingTestMain.compareResults(new File("/home/sitt/Documents/idlab/rml-streamer/src/test/resources/stream/"), TestSink.getTriples.filter(!_.isEmpty))
    TestSink.empty()
    succeed
  }

}
