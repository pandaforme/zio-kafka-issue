package com.test.helper

import com.test.model.config.{KafkaConsumerConfig, KafkaProducerConfig}
import io.jvm.uuid.UUID
import zio.{ZLayer, _}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.config.Config
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde

object KafkaHelper {

  val getConsumer: ZLayer[Clock with Blocking with Config[KafkaConsumerConfig], Throwable, Consumer] =
    ZLayer
      .fromServiceManaged[KafkaConsumerConfig, Clock with Blocking, Throwable, Consumer.Service] {
        config: KafkaConsumerConfig =>
          val consumerSettings = ConsumerSettings(config.servers.map(_.value))
            .withGroupId(config.groupId.value)
            .withClientId(UUID.randomString)
            .withOffsetRetrieval(OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest))
          Consumer.make(consumerSettings)
      }.fresh

  def getProducer[K: Tag, V: Tag](
    keySerializer: Serde[Any, K],
    valueSerializer: Serde[Any, V]
  ): ZLayer[Config[KafkaProducerConfig], Throwable, Producer[Any, K, V]] =
    ZLayer.fromServiceManaged { config: KafkaProducerConfig =>
      val producerSettings = ProducerSettings(config.servers.map(_.value))
      Producer.make(producerSettings, keySerializer, valueSerializer)
    }.fresh
}
