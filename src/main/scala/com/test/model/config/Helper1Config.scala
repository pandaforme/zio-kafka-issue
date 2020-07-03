package com.test.model.config

import zio.config.ConfigDescriptor
import zio.config.ConfigDescriptor._

final case class Helper1Config(
  consumer: KafkaConsumerConfig,
  producer: KafkaProducerConfig
)

object Helper1Config {

  def configDescriptor: ConfigDescriptor[Helper1Config] =
    (
      nested("consumer")(KafkaConsumerConfig.configDescriptor) |@|
        nested("producer")(KafkaProducerConfig.configDescriptor)
    )(
      Helper1Config.apply,
      Helper1Config.unapply
    )
}
