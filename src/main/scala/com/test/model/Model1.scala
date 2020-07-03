package com.test.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

final case class Model1(
  id: String,
  bookingId: Long,
  hotelId: Long,
  eventId: String,
  execTime: Long,
  recModifiedWhen: Long
)

object Model1 {
  import nequi.circe.kafka._

  implicit val decoder: Decoder[Model1] = deriveDecoder[Model1]
  implicit val encoder: Encoder[Model1] = deriveEncoder[Model1]

  val serializer: Serializer[Model1] = implicitly
  val deserializer: Deserializer[Model1] = implicitly
  val serde: Serde[Model1] = implicitly
}
