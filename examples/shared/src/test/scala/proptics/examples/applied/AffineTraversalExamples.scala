package proptics.examples.applied

import cats.syntax.option._

import proptics.instances.all._
import proptics.specs.PropticsSuite
import proptics.syntax.applied.all._

class AffineTraversalExamples extends PropticsSuite {
  test("preview the head of a list within a tuple") {
    val result = (9, List("head", "?", "?")).second.headOption

    assertResult("head".some)(result.preview)
  }

  test("transform each head of a nested list to upper case") {
    val target = List(List("a", "b", "c"), List("b", "c", "d"), Nil)
    val result = target.traversal.headOption.over(_.toUpperCase)

    assertResult(List(List("A", "b", "c"), List("B", "c", "d"), Nil))(result)
  }

  test("transform each tail of a nested list to upper case") {
    val target = List(List("a", "b", "c"), List("b", "c", "d"), Nil)
    val result = target.traversal.tailOption.over(_.map(_.toUpperCase))

    assertResult(List(List("a", "B", "C"), List("b", "C", "D"), Nil))(result)
  }

  test("remove the suffix or prefix of a string") {
    val suffixed1 = ("suffixed", 9).first.suffixed("fixed")
    val prefixed1 = ("prefixed", 9).first.prefixed("pre")
    val suffixed2 = ("prefixed", 9).first.suffixed("fixed")
    val prefixed2 = ("suffixed", 9).first.prefixed("pre")

    assertResult("suf".some)(suffixed1.preview)
    assertResult("pre".some)(suffixed2.preview)
    assertResult("fixed".some)(prefixed1.preview)
    assertResult(None)(prefixed2.preview)
  }

  test("using index to get an element of a List") {
    val head = List(List(9, 2, 3)).index(0).headOption

    assertResult(Some(9))(head.preview)
  }
}
