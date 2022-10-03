package com.permutive.metrics

import cats.{Applicative, Eq, Hash, Order, Show, ~>}

sealed abstract class Gauge[F[_]] { self =>

  def inc(n: Double = 1.0): F[Unit]
  def dec(n: Double = 1.0): F[Unit]
  def set(n: Double): F[Unit]

  def setToCurrentTime(): F[Unit]

  final def mapK[G[_]](fk: F ~> G): Gauge[G] = new Gauge[G] {
    override def inc(n: Double): G[Unit] = fk(self.inc(n))

    override def dec(n: Double): G[Unit] = fk(self.dec(n))

    override def set(n: Double): G[Unit] = fk(self.set(n))

    override def setToCurrentTime(): G[Unit] = fk(self.setToCurrentTime())
  }

}

/** Escape hatch for writing testing implementations in `metrics-testing` module
  */
abstract private[metrics] class Gauge_[F[_]] extends Gauge[F]

object Gauge {

  final class Name private (val value: String) extends AnyVal {
    override def toString: String = value
  }

  object Name extends GaugeNameFromStringLiteral {

    final private val regex = "^[a-zA-Z_:][a-zA-Z0-9_:]*$".r

    def from(string: String): Either[String, Name] =
      Either.cond(
        regex.matches(string),
        new Name(string),
        s"$string must match `$regex`"
      )

    implicit val GaugeNameHash: Hash[Name] = Hash.by(_.value)

    implicit val GaugeNameEq: Eq[Name] = Eq.by(_.value)

    implicit val GaugeNameShow: Show[Name] = Show.show(_.value)

    implicit val GaugeNameOrder: Order[Name] = Order.by(_.value)

  }

  def make[F[_]](
      _inc: Double => F[Unit],
      _dec: Double => F[Unit],
      _set: Double => F[Unit],
      _setToCurrentTime: F[Unit]
  ): Gauge[F] = new Gauge[F] {
    override def inc(n: Double): F[Unit] = _inc(n)

    override def dec(n: Double): F[Unit] = _dec(n)

    override def set(n: Double): F[Unit] = _set(n)

    override def setToCurrentTime(): F[Unit] = _setToCurrentTime
  }

  def noop[F[_]: Applicative] = new Gauge[F] {
    override def inc(n: Double): F[Unit] = Applicative[F].unit

    override def dec(n: Double): F[Unit] = Applicative[F].unit

    override def set(n: Double): F[Unit] = Applicative[F].unit

    override def setToCurrentTime(): F[Unit] = Applicative[F].unit
  }

  abstract class Labelled[F[_], A] {
    self =>

    def inc(n: Double = 1.0, labels: A): F[Unit]

    def dec(n: Double = 1.0, labels: A): F[Unit]

    def set(n: Double, labels: A): F[Unit]

    def setToCurrentTime(labels: A): F[Unit]

    final def mapK[G[_]](fk: F ~> G): Labelled[G, A] =
      new Labelled[G, A] {
        override def inc(n: Double, labels: A): G[Unit] = fk(
          self.inc(n, labels)
        )

        override def dec(n: Double, labels: A): G[Unit] = fk(
          self.dec(n, labels)
        )

        override def set(n: Double, labels: A): G[Unit] = fk(
          self.set(n, labels)
        )

        override def setToCurrentTime(labels: A): G[Unit] = fk(
          self.setToCurrentTime(labels)
        )
      }

  }

  /** Escape hatch for writing testing implementations in `metrics-testing`
    * module
    */
  abstract private[metrics] class Labelled_[F[_], A] extends Labelled[F, A]

  object Labelled {
    def make[F[_], A](
        _inc: (Double, A) => F[Unit],
        _dec: (Double, A) => F[Unit],
        _set: (Double, A) => F[Unit],
        _setToCurrentTime: A => F[Unit]
    ): Labelled[F, A] = new Labelled[F, A] {
      override def inc(n: Double, labels: A): F[Unit] = _inc(n, labels)

      override def dec(n: Double, labels: A): F[Unit] = _dec(n, labels)

      override def set(n: Double, labels: A): F[Unit] = _set(n, labels)

      override def setToCurrentTime(labels: A): F[Unit] = _setToCurrentTime(
        labels
      )
    }

    def noop[F[_]: Applicative, A]: Labelled[F, A] = new Labelled[F, A] {
      override def inc(n: Double, labels: A): F[Unit] = Applicative[F].unit

      override def dec(n: Double, labels: A): F[Unit] = Applicative[F].unit

      override def set(n: Double, labels: A): F[Unit] = Applicative[F].unit

      override def setToCurrentTime(labels: A): F[Unit] = Applicative[F].unit
    }
  }
}
