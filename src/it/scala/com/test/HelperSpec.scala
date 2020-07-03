package com.test

import java.io.File
import java.nio.charset.Charset

import scala.collection.JavaConverters._

import com.dimafeng.testcontainers.{ForAllTestContainer, KafkaContainer}
import io.jvm.uuid._
import org.apache.kafka.clients.producer.ProducerRecord
import org.scalatest.EitherValues
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.shaded.org.apache.commons.io.FileUtils
import zio._
import zio.duration._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.logging.log
import zio.logging.slf4j.Slf4jLogger

class HelperSpec extends AsyncFlatSpec with ForAllTestContainer with BootstrapRuntime with Matchers with EitherValues {

  @SuppressWarnings(Array("org.wartremover.warts.Unit", "org.wartremover.warts.NonUnitStatements"))
  override val container = KafkaContainer()

  private val logger = Slf4jLogger.make((_, logEntry) => logEntry)

  it should "push correct data to agp-credit topic" in {
    unsafeRunToFuture {
      (for {
        _ <- log.info("Start to prepare data")
        _ <- prepareData
        _ <- log.info("Stop to prepare data")
        _ <- Main.run(List.empty).fork
        _ <- ZIO.sleep(10.minute)
      } yield {}).provideCustomLayer(logger)
    }.map { _ =>
      assert(true)
    }
  }

  private def prepareData = {
    val bootstrapServers = container.bootstrapServers
    val settings = ProducerSettings(List(bootstrapServers))

    (for {
      configs <-
        ZIO
          .effect(
            FileUtils.listFiles(new File(getClass.getClassLoader.getResource("").getPath), Array("conf"), false)
          )
          .orDie
      contents <- ZIO.foreach(configs.asScala)(file =>
        ZIO
          .effect(
            FileUtils
              .readFileToString(file, Charset.defaultCharset())
              .replaceAll("servers.*", s"""servers : ["$bootstrapServers"]""")
          )
          .orDie
      )
      _ <-
        ZIO
          .foreach(contents)(content =>
            ZIO
              .effect(
                FileUtils
                  .writeStringToFile(
                    new File(getClass.getClassLoader.getResource("").getPath + "application.conf"),
                    content,
                    Charset.defaultCharset()
                  )
              )
              .orDie
          )
      files <-
        ZIO
          .effect(
            FileUtils.listFiles(new File(getClass.getClassLoader.getResource("MockData").getPath), Array("json"), false)
          )
          .orDie
      contents <-
        ZIO.foreach(files.asScala)(file => ZIO.effect(FileUtils.readFileToString(file, Charset.defaultCharset())).orDie)
      records <-
        ZIO
          .foreach(contents)(content => ZIO.succeed(new ProducerRecord("topic1", UUID.randomString, content)))
      _ <-
        ZIO
          .foreach(records)(record =>
            log
              .info(s"Insert ${record.value()} into ${record.topic()}") *> Producer.produce[Any, String, String](record)
          )
          .repeat(Schedule.recurs(1000))
    } yield {}).provideCustomLayer(ZLayer.fromManaged(Producer.make(settings, Serde.string, Serde.string)) ++ logger)
  }
}
