package proptics.syntax

import cats.arrow.{Profunctor, Strong}
import cats.instances.function._
import proptics.Setter_
import proptics.internal.Indexed
import proptics.iso.Newtype.newtype
import proptics.newtype.Newtype
import proptics.newtype.Newtype.Aux

object IndexedSyntax {
  implicit class IndexedOps[P[_, _], I, S, T, R](val indexed: Indexed[P, I, S, T => R]) extends AnyVal {
    def reindexed[J](f: I => J)(implicit ev0: Profunctor[P], ev1: Newtype.Aux[(J, S), (I, S)]): Indexed[P, J, S, T => R] = {
      val firstIndex = Strong[* => *].first[I, J, S](f)
      val setter = Setter_[P[(I, S), T => R], P[(J, S), T => R], (I, S), (J, S)](is2js => pistr => {
        val aux: Aux[(I, S), (J, S)] = Newtype.newtype(ev1.unwrap)(is2js)

        ev0.lmap(pistr)(js => newtype(ev1)(aux).set(js)(js))
      })

      Indexed(setter.over(firstIndex)(indexed.runIndex))
    }
  }

}