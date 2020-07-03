package com.test.model

import zio.kafka.consumer.Offset

final case class Model2Record(key: String, agpCreditEnriched: Option[Model2], offset: Offset)
