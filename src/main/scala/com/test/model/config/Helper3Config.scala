package com.test.model.config

import zio.config.ConfigDescriptor
import zio.config.ConfigDescriptor.nested

final case class Helper3Config(consumer: KafkaConsumerConfig)

object Helper3Config {

  def configDescriptor: ConfigDescriptor[Helper3Config] =
    nested("consumer")(KafkaConsumerConfig.configDescriptor)(
      Helper3Config.apply,
      Helper3Config.unapply
    )
}
