package com.test.helper

import com.test.model.config.Helper1Config
import com.test.model.{Model1, Model1Record}
import io.circe.syntax._
import org.apache.kafka.clients.producer.ProducerRecord
import zio.ZIO
import zio.blocking.Blocking
import zio.clock.Clock
import zio.config.{config, Config}
import zio.kafka.consumer.{Consumer, Offset, Subscription}
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.logging.{log, Logging}

object Helper1 {

  val consumeAndProduce: ZIO[Clock with Blocking with Config[Helper1Config] with Consumer with Producer[
    Any,
    String,
    String
  ] with Logging, Throwable, Unit] =
    for {
      config <- config[Helper1Config]
      _ <-
        Consumer
          .subscribeAnd(Subscription.topics(config.consumer.topic.value))
          .plainStream(Serde.string, Serde(Model1.serde).asEither)
          .mapM { record =>
            val key: String = record.key
            val offset: Offset = record.offset
            for {
              option1 <- record.record.value() match {
                case Right(s) =>
                  ZIO.some(s)
                case Left(errorMessage: String) =>
                  log.error(s"Can not parse this event, $errorMessage") *> ZIO.none
              }
            } yield Model1Record(option1, key, offset)
          }
          .mapM { record =>
            record.transaction match {
              case Some(s) =>
                val value = s.asJson.toString
                val topic = config.producer.topic.value
                log.info(s"Write $value into $topic") *>
                  Producer
                    .produce[Any, String, String](new ProducerRecord(topic, record.key, value))
                    .as(record.offset)
              case None =>
                ZIO.succeed(record.offset)
            }
          }
          .aggregateAsync(Consumer.offsetBatches)
          .mapM(_.commit)
          .runDrain
    } yield {}
}
