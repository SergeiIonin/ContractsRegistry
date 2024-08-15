import sbt.*
import sbt.Keys.libraryDependencies

object Dependencies {
  private object Versions {
    val catsEffects = "3.5.3"
    val circe = "0.14.9"
    val tapir = "1.10.14"
    val http4s = "0.23.27"
    val tapirYamlDoc = "0.20.2"
    val shapeless = "3.0.1"
    val testcontainers = "0.41.4"
    val scalatest = "3.2.19"
    val `specs2-cats` = "4.20.8"
  }

  lazy val catsDependencies =
    Seq(
      "org.typelevel" %% "cats-effect" % Versions.catsEffects,
      "org.typelevel" %% "cats-effect-kernel" % Versions.catsEffects,
      "org.typelevel" %% "cats-effect-std" % Versions.catsEffects,
      "org.typelevel" %% "cats-effect-testing-specs2" % "1.5.0" % Test,
      "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test
    )

  lazy val circeDependencies =
    Seq(
      "io.circe" %% "circe-core" % Versions.circe,
      "io.circe" %% "circe-generic" % Versions.circe,
      "io.circe" %% "circe-parser" % Versions.circe,
      "io.circe" %% "circe-literal" % Versions.circe
    )

  val schemaRegistry = Seq(
    //"io.confluent"    % "kafka-schema-registry"        % "7.6.2",
    "org.apache.avro" % "avro"                         % "1.11.3",
    //"io.confluent"    % "kafka-schema-registry"        % "6.2.0",
    "io.confluent"    % "kafka-schema-registry-client" % "7.6.2"
  )

/*  lazy val kafka = Seq(
    "org.apache.kafka" %% "kafka" % "3.7.1",
  )*/

  lazy val fs2 = Seq(
    "co.fs2" %% "fs2-core" % "3.10.2",
    "com.github.fd4s" %% "fs2-kafka" % "3.5.1"
  )

  lazy  val http4s = Seq(
    "org.http4s" %% "http4s-circe"        % Versions.http4s,
    "org.http4s" %% "http4s-ember-client" % Versions.http4s,
    "org.http4s" %% "http4s-ember-server" % Versions.http4s,
    "org.http4s" %% "http4s-dsl"          % Versions.http4s
  )

  lazy val tapirDependencies =
    Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % Versions.tapir,
      //"com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % Versions.tapirYamlDoc,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % "0.19.0-M4",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui" % Versions.tapir,
    )

  lazy val miscDependencies =
    Seq(
      "org.typelevel" %% "shapeless3-deriving" % Versions.shapeless,
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.7",
      "org.tpolecat" %% "skunk-core" % "0.6.4",
      "org.tpolecat" %% "skunk-circe" % "0.6.4",
      "com.47deg" %% "github4s" % "0.33.3",
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "io.scalaland" %% "chimney" % "1.3.0"
    )

  lazy val testDependencies =
    Seq(
      "org.scalatest" %% "scalatest"                     % Versions.scalatest % Test,
      "org.specs2"    %% "specs2-cats"                   % Versions.`specs2-cats` % Test,
    ) ++
    Seq("testcontainers-scala-core", "testcontainers-scala-kafka",
      "testcontainers-scala-scalatest", "testcontainers-scala-postgresql")
      .map("com.dimafeng" %% _ % Versions.testcontainers % Test)
}