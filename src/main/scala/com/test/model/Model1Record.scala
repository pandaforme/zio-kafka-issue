package com.test.model

import zio.kafka.consumer.Offset

final case class Model1Record(transaction: Option[Model1], key: String, offset: Offset)
