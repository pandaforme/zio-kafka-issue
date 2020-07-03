package com.test

import zio.kafka.serde.Deserializer

package object helper {
  implicit class DeserializerHelper[R, V](deserializer: Deserializer[R, V]) {

    def asEither: Deserializer[R, Either[String, V]] =
      Deserializer { (topic, headers, data) =>
        deserializer
          .deserialize(topic, headers, data)
          .fold(throwable => Left(s"Topic: $topic, Data: ${new String(data)}, Exception: $throwable"), Right(_))
      }
  }
}
