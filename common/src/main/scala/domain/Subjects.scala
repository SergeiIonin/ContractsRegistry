package io.github.sergeiionin.contractsregistrator
package domain

opaque type Subjects = List[String]

object Subjects:
  def apply(subjects: List[String]): Subjects = subjects

  def toList(subjects: Subjects): List[String] = subjects
