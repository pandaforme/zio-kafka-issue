package com.test.helper

import com.test.model.config.Helper1Config
import com.test.model.{Model1, Model1Record}
import io.circe.syntax._
import io.jvm.uuid.UUID
import org.apache.kafka.clients.producer.ProducerRecord
import zio.blocking.Blocking
import zio.clock.Clock
import zio.config.{config, Config}
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{Consumer, ConsumerSettings, Offset, Subscription}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.logging.{log, Logging}
import zio.{ZIO, ZLayer}

object Helper1 {

  val consumeAndProduce: ZIO[Clock with Blocking with Config[Helper1Config] with Logging, Throwable, Unit] =
    for {
      config <- config[Helper1Config]
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
          .provideSomeLayer[Clock with Blocking with Config[Helper1Config] with Logging](consumer ++ producer)
    } yield {}
}
