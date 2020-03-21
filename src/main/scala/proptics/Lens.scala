package proptics

import cats.arrow.Strong
import cats.syntax.option._
import cats.instances.function._
import cats.syntax.apply._
import cats.{Alternative, Comonad, Functor}
import proptics.internal.{Forget, Zipping}
import proptics.newtype.Disj
import proptics.profunctor.{Costar, Star}
import proptics.rank2types.Rank2TypeLensLike

import scala.Function.const

/**
 * Given a type whose "focus element" always exists,
 * a [[Lens]] provides a convenient way to view, set, and transform
 * that element.
 *
 * @tparam S the source of a [[Lens]]
 * @tparam T the modified source of a [[Lens]]
 * @tparam A the target of a [[Lens]]
 * @tparam B the modified target of a [[Lens]]
 */
abstract class Lens[S, T, A, B] extends Serializable { self =>
  private[proptics] def apply[P[_, _]](pab: P[A, B])(implicit ev: Strong[P]): P[S, T]

  def view(s: S): A = self[Forget[A, *, *]](Forget(identity[A])).runForget(s)

  def over(f: A => B): S => T = self(f)

  def set(b: B): S => T = over(const(b))

  def overF[F[_]: Functor](f: A => F[B])(s: S): F[T] = traverse(s)(f)

  def traverse[F[_]: Functor](s: S)(f: A => F[B]): F[T] = self(Star(f)).runStar(s)

  def filter(f: A => Boolean): S => Option[A] = s => view(s).some.filter(f)

  def exists(f: A => Boolean): S => Boolean = f compose view

  def noExists(f: A => Boolean): S => Boolean = s => !exists(f)(s)

  def failover[F[_]](f: A => B)(s: S)(implicit ev0: Strong[Star[(Disj[Boolean], *), *, *]], ev1: Alternative[F]): F[T] = {
    val star = Star[(Disj[Boolean], *), A, B](a => (Disj(true), f(a)))

    self(star).runStar(s) match {
      case (Disj(true), x) => ev1.pure(x)
      case (Disj(false), _) => ev1.empty
    }
  }

  def zipWith[F[_]](f: A => A => B): S => S => T = self(Zipping(f)).runZipping

  def zipWithF[F[_]: Comonad](fs: F[S])(f: F[A] => B): T = self(Costar(f)).runCostar(fs)
}

object Lens {
  private[proptics] def apply[S, T, A, B](f: Rank2TypeLensLike[S, T, A, B]): Lens[S, T, A, B] = new Lens[S, T, A, B] { self =>
    override def apply[P[_, _]](pab: P[A, B])(implicit ev: Strong[P]): P[S, T] = f(pab)
  }

  /**
   * Create a [[Lens]] from a getter/setter pair.
   */
  def apply[S, T, A, B](get: S => A)(set: S => B => T): Lens[S, T, A, B] = lens((get, set).mapN(Tuple2.apply))

  def lens[S, T, A, B](to: S => (A, B => T)): Lens[S, T, A, B] = Lens(new Rank2TypeLensLike[S, T, A, B] {
    override def apply[P[_, _]](pab: P[A, B])(implicit ev: Strong[P]): P[S, T] =
      liftOptic(to)(ev)(pab)
  })

  private[proptics] def liftOptic[P[_, _], S, T, A, B](to: S => (A, B => T))(implicit ev: Strong[P]): P[A, B] => P[S, T] =
    pab => ev.dimap(ev.first[A, B, B => T](pab))(to) { case (b, f) => f(b) }
}

object Lens_ {
  def apply[S, A](get: S => A)(set: S => A => S): Lens_[S, A] = Lens[S, S, A, A](get)(set)

  def lens[S, A](to: S => (A, A => S)): Lens_[S, A] = Lens.lens[S, S, A, A](to)
}
