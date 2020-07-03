package com.test.model.config

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import zio.config.ConfigDescriptor
import zio.config.ConfigDescriptor.{list, string}
import zio.config.refined.nonEmpty

final case class KafkaProducerConfig(servers: List[Refined[String, NonEmpty]], topic: Refined[String, NonEmpty])

object KafkaProducerConfig {

  val configDescriptor: ConfigDescriptor[KafkaProducerConfig] = (
    list("servers")(nonEmpty(string)) |@|
      nonEmpty(string("topic"))
  )(KafkaProducerConfig.apply, KafkaProducerConfig.unapply)
}
