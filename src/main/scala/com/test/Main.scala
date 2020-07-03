package com.test

import com.test.helper.{Helper1, Helper2, Helper3}
import com.test.model.config.DummyServiceConfig
import zio.config.syntax._
import zio.config.typesafe.TypesafeConfig
import zio.logging.log
import zio.logging.slf4j.Slf4jLogger
import zio.{App, ExitCode, ZIO, _}

object Main extends App {
  private val logger = Slf4jLogger.make((_, logEntry) => logEntry)

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    // config
    val config = TypesafeConfig.fromDefaultLoader[DummyServiceConfig](DummyServiceConfig.configDescriptor)
    val helper1Config = config.narrow(_.helper1Config)
    val helper2Config = config.narrow(_.helper2Config)
    val helper3Config = config.narrow(_.helper3Config)

    // helper
    val helper1 = {
      helper1Config ++ logger
    }

    val helper2 = {
      helper2Config ++ logger
    }

    val helper3 = {
      helper3Config ++ logger
    }

    ZIO
      .collectAllPar(
        List(
          Helper1.consumeAndProduce,
          Helper2.consumeAndProduce,
          Helper3.consume
        )
      )
      .provideCustomLayer(
        helper1 ++ helper2 ++ helper3
      )
      .foldM(
        failure => log.throwable(s"Something wrong", failure) *> ZIO.succeed(ExitCode.failure),
        _ => ZIO.succeed(ExitCode.success)
      )
      .provideCustomLayer(logger)
  }
}
