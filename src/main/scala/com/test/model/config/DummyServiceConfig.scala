package com.test.model.config

import zio.config.ConfigDescriptor
import zio.config.ConfigDescriptor.nested

final case class DummyServiceConfig(
  helper1Config: Helper1Config,
  helper2Config: Helper2Config,
  helper3Config: Helper3Config
)

object DummyServiceConfig {

  val configDescriptor: ConfigDescriptor[DummyServiceConfig] =
    (nested("help1")(Helper1Config.configDescriptor) |@|
      nested("help2")(Helper2Config.configDescriptor) |@|
      nested("help3")(Helper3Config.configDescriptor))(
      DummyServiceConfig.apply,
      DummyServiceConfig.unapply
    )
}
