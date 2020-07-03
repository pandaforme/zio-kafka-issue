import BuildEnvPlugin.autoImport.{buildEnv, BuildEnv}
import com.typesafe.sbt.packager.docker.Cmd
import sbt.Def

addCompilerPlugin(scalafixSemanticdb)
enablePlugins(JavaAppPackaging, DockerPlugin, BuildEnvPlugin)
addCommandAlias("compileAll", "; compile; test:compile; it:compile")
addCommandAlias("fix", "; compile:scalafix OrganizeImports; test:scalafix OrganizeImports; it:scalafix OrganizeImports")
addCommandAlias("fmt", "all scalafmt scalafmtSbt compile:scalafmt test:scalafmt it:scalafmt")
addCommandAlias(
  "fix-check",
  "; compile:scalafix OrganizeImports --check; test:scalafix OrganizeImports --check; it:scalafix OrganizeImports --check")
addCommandAlias("fmt-check", "all scalafmtCheck compile:scalafmtCheck test:scalafmtCheck it:scalafmtCheck")

lazy val root =
  (project in file("."))
    .settings(
      organization                                                                      := "com.test",
      name                                                                              := "helper",
      scalaVersion                                                                      := "2.12.11",
      scalafmtOnCompile                                                                 := true,
      fork in Test                                                                      := true,
      fork in Runtime                                                                   := true,
      fork in IntegrationTest                                                           := true,
      testFrameworks                                                                    := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      testFrameworks in IntegrationTest                                                 := Seq(TestFrameworks.ScalaTest),
      scalafixDependencies in ThisBuild += "com.github.liancheng" %% "organize-imports" % "0.3.0",
      //see: https://github.com/scalameta/scalameta/issues/1785
      scalacOptions += s"-P:semanticdb:sourceroot:${baseDirectory.in(ThisBuild).value}",
      wartremoverErrors ++= Warts
        .allBut(Wart.Nothing, Wart.Any, Wart.Product, Wart.Serializable, Wart.ImplicitParameter)
    )
    .configs(IntegrationTest)
    .settings(
      Defaults.itSettings,
      inConfig(IntegrationTest)(
        scalafixConfigSettings(IntegrationTest) ++ org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings)
    )
    .settings(
      // Scala libs
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio"                                 % "1.0.0-RC21-2",
        "dev.zio" %% "zio-streams"                         % "1.0.0-RC21-2",
        "dev.zio" %% "zio-kafka"                           % "0.11.0",
        "dev.zio" %% "zio-config"                          % "1.0.0-RC23-1",
        "dev.zio" %% "zio-config-refined"                  % "1.0.0-RC23-1",
        "dev.zio" %% "zio-config-typesafe"                 % "1.0.0-RC23-1",
        "dev.zio" %% "zio-logging"                         % "0.3.2",
        "dev.zio" %% "zio-logging-slf4j"                   % "0.3.2",
        "com.nequissimus" %% "circe-kafka"                 % "2.4.0",
        "io.circe" %% "circe-generic"                      % "0.13.0",
        "io.jvm.uuid" %% "scala-uuid"                      % "0.3.1",
        "dev.zio" %% "zio-interop-cats"                    % "2.0.0.0-RC13",
        "dev.zio" %% "zio-test"                            % "1.0.0-RC21-2" % Test,
        "dev.zio" %% "zio-test-sbt"                        % "1.0.0-RC21-2" % Test,
        "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.37.0" % IntegrationTest,
        "com.dimafeng" %% "testcontainers-scala-kafka"     % "0.37.0" % IntegrationTest,
        "org.scalatest" %% "scalatest"                     % "3.2.0" % IntegrationTest
      ),
      // Java libs
      libraryDependencies ++= Seq(
        "com.typesafe"   % "config"          % "1.4.0",
        "ch.qos.logback" % "logback-classic" % "1.2.3",
        "org.slf4j"      % "slf4j-api"       % "1.7.30"
      )
    )
