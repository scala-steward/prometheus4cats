/*
 * Copyright 2022 Permutive
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package openmetrics4s

import cats.{Applicative, Contravariant, Eq, Hash, Order, Show, ~>}

sealed abstract class Gauge[F[_], A] { self =>

  def inc: F[Unit]
  def inc(n: A): F[Unit]
  def dec: F[Unit]
  def dec(n: A): F[Unit]
  def set(n: A): F[Unit]

  def setToCurrentTime(): F[Unit]

  def contramap[B](f: B => A): Gauge[F, B] = new Gauge[F, B] {
    override def inc: F[Unit] = self.inc

    override def inc(n: B): F[Unit] = self.inc(f(n))

    override def dec: F[Unit] = self.dec

    override def dec(n: B): F[Unit] = self.dec(f(n))

    override def set(n: B): F[Unit] = self.set(f(n))

    override def setToCurrentTime(): F[Unit] = self.setToCurrentTime()

  }

  final def mapK[G[_]](fk: F ~> G): Gauge[G, A] = new Gauge[G, A] {
    override def inc: G[Unit] = fk(self.inc)

    override def inc(n: A): G[Unit] = fk(self.inc(n))

    override def dec: G[Unit] = fk(self.dec)

    override def dec(n: A): G[Unit] = fk(self.dec(n))

    override def set(n: A): G[Unit] = fk(self.set(n))

    override def setToCurrentTime(): G[Unit] = fk(self.setToCurrentTime())
  }

}

object Gauge {

  /** Refined value class for a gauge name that has been parsed from a string
    */
  final class Name private (val value: String) extends AnyVal {
    override def toString: String = s"""Gauge.Name("$value")"""
  }

  object Name extends GaugeNameFromStringLiteral {

    final private val regex = "^[a-zA-Z_:][a-zA-Z0-9_:]*$".r.pattern

    /** Parse a [[Name]] from the given string
      * @param string
      *   value from which to parse a gauge name
      * @return
      *   a parsed [[Name]] or failure message, represented by an [[scala.Either]]
      */
    def from(string: String): Either[String, Name] =
      Either.cond(
        regex.matcher(string).matches(),
        new Name(string),
        s"$string must match `$regex`"
      )

    implicit val catsInstances: Hash[Name] with Order[Name] with Show[Name] = new Hash[Name]
      with Order[Name]
      with Show[Name] {
      override def hash(x: Name): Int = Hash[String].hash(x.value)

      override def compare(x: Name, y: Name): Int = Order[String].compare(x.value, y.value)

      override def show(t: Name): String = t.value

      override def eqv(x: Name, y: Name): Boolean = Eq[String].eqv(x.value, y.value)
    }

  }

  implicit def catsInstances[F[_]]: Contravariant[Gauge[F, *]] = new Contravariant[Gauge[F, *]] {
    override def contramap[A, B](fa: Gauge[F, A])(f: B => A): Gauge[F, B] = fa.contramap(f)
  }

  def make[F[_], A](
      default: A,
      _inc: A => F[Unit],
      _dec: A => F[Unit],
      _set: A => F[Unit],
      _setToCurrentTime: F[Unit]
  ): Gauge[F, A] = new Gauge[F, A] {
    override def inc: F[Unit] = inc(default)

    override def inc(n: A): F[Unit] = _inc(n)

    override def dec: F[Unit] = dec(default)

    override def dec(n: A): F[Unit] = _dec(n)

    override def set(n: A): F[Unit] = _set(n)

    override def setToCurrentTime(): F[Unit] = _setToCurrentTime
  }

  def noop[F[_]: Applicative, A]: Gauge[F, A] = new Gauge[F, A] {
    override def inc: F[Unit] = Applicative[F].unit

    override def inc(n: A): F[Unit] = Applicative[F].unit

    override def dec: F[Unit] = Applicative[F].unit

    override def dec(n: A): F[Unit] = Applicative[F].unit

    override def set(n: A): F[Unit] = Applicative[F].unit

    override def setToCurrentTime(): F[Unit] = Applicative[F].unit
  }

  abstract class Labelled[F[_], A, B] {
    self =>

    def inc(labels: B): F[Unit]

    def inc(n: A, labels: B): F[Unit]

    def dec(labels: B): F[Unit]

    def dec(n: A, labels: B): F[Unit]

    def set(n: A, labels: B): F[Unit]

    def setToCurrentTime(labels: B): F[Unit]

    def contramap[C](f: C => A): Labelled[F, C, B] = new Labelled[F, C, B] {
      override def inc(labels: B): F[Unit] = self.inc(labels)

      override def inc(n: C, labels: B): F[Unit] = self.inc(f(n), labels)

      override def dec(labels: B): F[Unit] = self.dec(labels)

      override def dec(n: C, labels: B): F[Unit] = self.dec(f(n), labels)

      override def set(n: C, labels: B): F[Unit] = self.set(f(n), labels)

      override def setToCurrentTime(labels: B): F[Unit] = self.setToCurrentTime(labels)
    }

    final def mapK[G[_]](fk: F ~> G): Labelled[G, A, B] =
      new Labelled[G, A, B] {

        override def inc(labels: B): G[Unit] = fk(self.inc(labels))

        override def inc(n: A, labels: B): G[Unit] = fk(
          self.inc(n, labels)
        )

        override def dec(labels: B): G[Unit] = fk(self.dec(labels))

        override def dec(n: A, labels: B): G[Unit] = fk(
          self.dec(n, labels)
        )

        override def set(n: A, labels: B): G[Unit] = fk(
          self.set(n, labels)
        )

        override def setToCurrentTime(labels: B): G[Unit] = fk(
          self.setToCurrentTime(labels)
        )

      }

  }

  object Labelled {
    implicit def catsInstances[F[_], C]: Contravariant[Labelled[F, *, C]] = new Contravariant[Gauge.Labelled[F, *, C]] {
      override def contramap[A, B](fa: Labelled[F, A, C])(f: B => A): Labelled[F, B, C] = fa.contramap(f)
    }

    def make[F[_], A, B](
        default: A,
        _inc: (A, B) => F[Unit],
        _dec: (A, B) => F[Unit],
        _set: (A, B) => F[Unit],
        _setToCurrentTime: B => F[Unit]
    ): Labelled[F, A, B] = new Labelled[F, A, B] {
      override def inc(labels: B): F[Unit] = inc(default, labels)

      override def inc(n: A, labels: B): F[Unit] = _inc(n, labels)

      override def dec(labels: B): F[Unit] = dec(default, labels)

      override def dec(n: A, labels: B): F[Unit] = _dec(n, labels)

      override def set(n: A, labels: B): F[Unit] = _set(n, labels)

      override def setToCurrentTime(labels: B): F[Unit] = _setToCurrentTime(
        labels
      )
    }

    def noop[F[_]: Applicative, A, B]: Labelled[F, A, B] = new Labelled[F, A, B] {
      override def inc(labels: B): F[Unit] = Applicative[F].unit

      override def inc(n: A, labels: B): F[Unit] = Applicative[F].unit

      override def dec(labels: B): F[Unit] = Applicative[F].unit

      override def dec(n: A, labels: B): F[Unit] = Applicative[F].unit

      override def set(n: A, labels: B): F[Unit] = Applicative[F].unit

      override def setToCurrentTime(labels: B): F[Unit] = Applicative[F].unit
    }
  }
}
