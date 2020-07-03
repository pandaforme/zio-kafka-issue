package com.test.model.config

import zio.config.ConfigDescriptor
import zio.config.ConfigDescriptor._

final case class Helper2Config(
  consumer: KafkaConsumerConfig,
  producer: KafkaProducerConfig
)

object Helper2Config {

  def configDescriptor: ConfigDescriptor[Helper2Config] =
    (nested("consumer")(KafkaConsumerConfig.configDescriptor) |@| nested("producer") {
      KafkaProducerConfig.configDescriptor
    })(Helper2Config.apply, Helper2Config.unapply)
}
