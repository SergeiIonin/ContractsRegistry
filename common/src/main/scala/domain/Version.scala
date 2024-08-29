package io.github.sergeiionin.contractsregistrator
package domain

opaque type Version = Int
object Version:
  def apply(version: Int): Version = version
  def toInt(version: Version): Int = version

opaque type Versions = List[Int]
object Versions:
  def apply(versions: List[Int]): Versions = versions
  def toList(versions: Versions): List[Int] = versions

extension (versions: Versions)
  def headOption: Option[Int] = versions.headOption
  def max: Int = versions.max
