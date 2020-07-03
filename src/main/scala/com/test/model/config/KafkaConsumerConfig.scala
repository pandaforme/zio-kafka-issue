package com.test.model.config

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import zio.config.ConfigDescriptor
import zio.config.ConfigDescriptor.{list, string}
import zio.config.refined.nonEmpty

final case class KafkaConsumerConfig(
  servers: List[Refined[String, NonEmpty]],
  groupId: Refined[String, NonEmpty],
  topic: Refined[String, NonEmpty]
)

object KafkaConsumerConfig {

  val configDescriptor: ConfigDescriptor[KafkaConsumerConfig] = (
    list("servers")(nonEmpty(string)) |@|
      nonEmpty(string("group-id")) |@|
      nonEmpty(string("topic"))
  )(KafkaConsumerConfig.apply, KafkaConsumerConfig.unapply)
}
