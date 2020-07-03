package com.test.helper

import com.test.model.Model2
import com.test.model.config.Helper3Config
import io.jvm.uuid.UUID
import zio.blocking.Blocking
import zio.clock.Clock
import zio.config.{config, Config}
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.serde.Serde
import zio.logging.{log, Logging}
import zio.{ZIO, ZLayer}

object Helper3 {

  val consume: ZIO[Clock with Blocking with Logging with Config[Helper3Config], Throwable, Unit] =
    for {
      config <- config[Helper3Config]
      consumerSettings = ConsumerSettings(config.consumer.servers.map(_.value))
        .withGroupId(config.consumer.groupId.value)
        .withClientId(UUID.randomString)
        .withOffsetRetrieval(OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest))
      consumer = ZLayer.fromManaged(Consumer.make(consumerSettings))
      _ <-
        Consumer
          .subscribeAnd(Subscription.topics(config.consumer.topic.value))
          .plainStream(Serde.string, Serde(Model2.serde).asEither)
          .mapM { record =>
            val offset = record.offset
            record.record.value() match {
              case Right(r) =>
                log.info(s"Get $r from ${config.consumer.topic.value}") *> ZIO.succeed(offset)
              case Left(errorMessage: String) =>
                log
                  .error(s"Can not parse this event, $errorMessage")
                  .as(offset)
            }
          }
          .aggregateAsync(Consumer.offsetBatches)
          .mapM(_.commit)
          .runDrain
          .provideSomeLayer[Clock with Blocking with Logging with Config[Helper3Config]](consumer)
    } yield {}
}
