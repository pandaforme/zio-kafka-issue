package com.test.helper

import com.test.model.config.Helper2Config
import com.test.model.{Model1, Model2, Model2Record}
import io.circe.syntax._
import io.jvm.uuid.UUID
import org.apache.kafka.clients.producer.ProducerRecord
import zio.blocking.Blocking
import zio.clock.Clock
import zio.config.{Config, config}
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.logging.{Logging, log}
import zio.{ZIO, ZLayer}

object Helper2 {

  val consumeAndProduce: ZIO[Clock with Blocking with Logging with Config[Helper2Config], Throwable, Unit] =
    for {
      config <- config[Helper2Config]
      consumerSettings = ConsumerSettings(config.consumer.servers.map(_.value))
        .withGroupId(config.consumer.groupId.value)
        .withClientId(UUID.randomString)
        .withOffsetRetrieval(OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest))
      consumer = ZLayer.fromManaged(Consumer.make(consumerSettings))
      producerSettings = ProducerSettings(config.producer.servers.map(_.value))
      producer = ZLayer.fromManaged(Producer.make(producerSettings, Serde.string, Serde.string))
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
          .provideSomeLayer[Clock with Blocking with Logging with Config[Helper2Config]](consumer ++ producer)
    } yield {}
}
