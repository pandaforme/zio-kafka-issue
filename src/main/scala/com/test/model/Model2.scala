package com.test.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

final case class Model2(
  id: String,
  bookingId: Long,
  hotelId: Long,
  eventId: String,
  execTime: Long,
  amount: Double,
  currency: String,
  recModifiedWhen: Long
)

object Model2 {
  //This import is just way too important to be hidden in the standard imports...
  import nequi.circe.kafka._

  implicit val decoder: Decoder[Model2] = deriveDecoder[Model2]
  implicit val encoder: Encoder[Model2] = deriveEncoder[Model2]

  val serializer: Serializer[Model2] = implicitly
  val deserializer: Deserializer[Model2] = implicitly
  val serde: Serde[Model2] = implicitly
}
