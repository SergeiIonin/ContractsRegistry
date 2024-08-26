package io.github.sergeiionin.contractsregistrator
package consumers

import cats.data.NonEmptyList

trait Consumer[F[_]]:
  def subscribe(topics: NonEmptyList[String]): F[Unit]
  def process(): F[Unit]
