package com.test.helper

import com.test.model.config.Helper2Config
import com.test.model.{Model1, Model2, Model2Record}
import io.circe.syntax._
import org.apache.kafka.clients.producer.ProducerRecord
import zio.ZIO
import zio.blocking.Blocking
import zio.clock.Clock
import zio.config.{config, Config}
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.logging.{log, Logging}

object Helper2 {

  val consumeAndProduce: ZIO[Clock with Blocking with Logging with Config[
    Helper2Config
  ] with Consumer with Producer[Any, String, String], Throwable, Unit] =
    for {
      config <- config[Helper2Config]
      _ <-
        Consumer
          .subscribeAnd(Subscription.topics(config.consumer.topic.value))
          .plainStream(Serde.string, Serde(Model1.serde).asEither)
          .mapM { record =>
            val offset = record.offset
            val key = record.key
            for {
              agpCreditEnriched <- record.record.value() match {
                case Right(s) =>
                  ZIO.some(
                    Model2(
                      s.id,
                      s.bookingId,
                      s.hotelId,
                      s.eventId,
                      s.execTime,
                      0,
                      "THB",
                      s.recModifiedWhen
                    )
                  )
                case Left(errorMessage: String) =>
                  log.error(s"Can not parse this event, $errorMessage") *> ZIO.none
              }
            } yield Model2Record(key, agpCreditEnriched, offset)
          }
          .mapM { record =>
            record.agpCreditEnriched match {
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
