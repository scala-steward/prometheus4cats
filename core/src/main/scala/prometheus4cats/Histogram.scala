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

package prometheus4cats

import cats.data.NonEmptySeq
import cats.syntax.flatMap._
import cats.{Applicative, Contravariant, FlatMap, ~>}

import java.util.regex.Pattern

sealed abstract class Histogram[F[_], -A] extends Metric[A] { self =>

  def observe(n: A): F[Unit]

  def contramap[B](f: B => A): Histogram[F, B] = new Histogram[F, B] {
    override def observe(n: B): F[Unit] = self.observe(f(n))
  }

  final def mapK[G[_]](fk: F ~> G): Histogram[G, A] = new Histogram[G, A] {
    override def observe(n: A): G[Unit] = fk(self.observe(n))
  }
}

object Histogram {

  /** A value that is produced by a histogram
    *
    * @note
    *   the size `bucketValues` '''MUST MATCH''' that of the number of buckets defined when creating the histogram in
    *   [[MetricFactory.WithCallbacks]]. If they do not match, the histogram may not render correctly or at all.
    *
    * @param sum
    *   the histogram sum
    * @param bucketValues
    *   values corresponding to to buckets defined when creating the histogram
    * @tparam A
    *   number type for this histogram value
    */
  case class Value[A](sum: A, bucketValues: NonEmptySeq[A]) {
    def map[B](f: A => B): Value[B] = Value(f(sum), bucketValues.map(f))
  }

  val DefaultHttpBuckets: NonEmptySeq[Double] =
    NonEmptySeq.of(0.005, .01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10)

  /** Refined value class for a histogram name that has been parsed from a string
    */
  final class Name private (val value: String) extends AnyVal with internal.Refined.Value[String] {
    override def toString: String = s"""Histogram.Name("$value")"""
  }

  object Name extends internal.Refined.StringRegexRefinement[Name] with internal.HistogramNameFromStringLiteral {
    override protected val regex: Pattern = "^[a-zA-Z_:][a-zA-Z0-9_:]*$".r.pattern
    override protected def make(a: String): Name = new Name(a)
  }

  implicit def catsInstances[F[_]]: Contravariant[Histogram[F, *]] = new Contravariant[Histogram[F, *]] {
    override def contramap[A, B](fa: Histogram[F, A])(f: B => A): Histogram[F, B] = fa.contramap(f)
  }

  def make[F[_], A](_observe: A => F[Unit]): Histogram[F, A] =
    new Histogram[F, A] {
      override def observe(n: A): F[Unit] = _observe(n)
    }

  def noop[F[_]: Applicative, A]: Histogram[F, A] =
    new Histogram[F, A] {
      override def observe(n: A): F[Unit] = Applicative[F].unit
    }

  sealed abstract class Exemplar[F[_], -A] extends Metric[A] {
    self =>

    def observe(n: A): F[Unit]

    def observeWithExemplar(n: A): F[Unit]

    def contramap[B](f: B => A): Exemplar[F, B] = new Exemplar[F, B] {
      override def observe(n: B): F[Unit] = self.observe(f(n))

      override def observeWithExemplar(n: B): F[Unit] = self.observeWithExemplar(f(n))
    }

    final def mapK[G[_]](fk: F ~> G): Exemplar[G, A] = new Exemplar[G, A] {
      override def observe(n: A): G[Unit] = fk(self.observe(n))

      override def observeWithExemplar(n: A): G[Unit] = fk(self.observeWithExemplar(n))
    }
  }

  object Exemplar {
    def make[F[_]: FlatMap: prometheus4cats.Exemplar, A](
        _observe: (A, Option[prometheus4cats.Exemplar.Labels]) => F[Unit]
    ): Exemplar[F, A] =
      new Exemplar[F, A] {
        override def observe(n: A): F[Unit] = _observe(n, None)

        override def observeWithExemplar(n: A): F[Unit] = prometheus4cats.Exemplar[F].get.flatMap(_observe(n, _))
      }

    private[prometheus4cats] def fromHistogram[F[_], A](histogram: Histogram[F, A]): Exemplar[F, A] =
      new Exemplar[F, A] {
        override def observe(n: A): F[Unit] = histogram.observe(n)

        override def observeWithExemplar(n: A): F[Unit] = histogram.observe(n)
      }

    def noop[F[_]: Applicative, A]: Exemplar[F, A] =
      new Exemplar[F, A] {
        override def observe(n: A): F[Unit] = Applicative[F].unit

        override def observeWithExemplar(n: A): F[Unit] = Applicative[F].unit
      }
  }

  sealed abstract class Labelled[F[_], -A, -B] extends Metric[A] with Metric.Labelled[B] {
    self =>

    def observe(n: A, labels: B): F[Unit]

    def contramap[C](f: C => A): Labelled[F, C, B] = new Labelled[F, C, B] {
      override def observe(n: C, labels: B): F[Unit] = self.observe(f(n), labels)
    }

    def contramapLabels[C](f: C => B): Labelled[F, A, C] = new Labelled[F, A, C] {
      override def observe(n: A, labels: C): F[Unit] = self.observe(n, f(labels))
    }

    final def mapK[G[_]](fk: F ~> G): Labelled[G, A, B] =
      new Labelled[G, A, B] {
        override def observe(n: A, labels: B): G[Unit] = fk(
          self.observe(n, labels)
        )
      }

  }

  object Labelled {
    implicit def catsInstances[F[_], C]: Contravariant[Labelled[F, *, C]] =
      new Contravariant[Labelled[F, *, C]] {
        override def contramap[A, B](fa: Labelled[F, A, C])(f: B => A): Labelled[F, B, C] = fa.contramap(f)
      }

    implicit def labelsContravariant[F[_], C]: LabelsContravariant[Labelled[F, C, *]] =
      new LabelsContravariant[Labelled[F, C, *]] {
        override def contramapLabels[A, B](fa: Labelled[F, C, A])(f: B => A): Labelled[F, C, B] = fa.contramapLabels(f)
      }

    def make[F[_], A, B](
        _observe: (A, B) => F[Unit]
    ): Labelled[F, A, B] =
      new Labelled[F, A, B] {
        override def observe(n: A, labels: B): F[Unit] =
          _observe(n, labels)
      }

    def noop[F[_]: Applicative, A, B]: Labelled[F, A, B] =
      new Labelled[F, A, B] {
        override def observe(n: A, labels: B): F[Unit] =
          Applicative[F].unit
      }

    sealed abstract class Exemplar[F[_], -A, -B] extends Metric[A] with Metric.Labelled[B] {
      self =>

      def observe(n: A, labels: B): F[Unit]

      def observeWithExemplar(n: A, labels: B): F[Unit]

      def contramap[C](f: C => A): Exemplar[F, C, B] = new Exemplar[F, C, B] {
        override def observe(n: C, labels: B): F[Unit] = self.observe(f(n), labels)

        override def observeWithExemplar(n: C, labels: B): F[Unit] = self.observeWithExemplar(f(n), labels)
      }

      def contramapLabels[C](f: C => B): Exemplar[F, A, C] = new Exemplar[F, A, C] {
        override def observe(n: A, labels: C): F[Unit] = self.observe(n, f(labels))

        override def observeWithExemplar(n: A, labels: C): F[Unit] = self.observeWithExemplar(n, f(labels))
      }

      final def mapK[G[_]](fk: F ~> G): Exemplar[G, A, B] =
        new Exemplar[G, A, B] {
          override def observe(n: A, labels: B): G[Unit] = fk(
            self.observe(n, labels)
          )

          override def observeWithExemplar(n: A, labels: B): G[Unit] = fk(
            self.observeWithExemplar(n, labels)
          )
        }

    }

    object Exemplar {
      implicit def catsInstances[F[_], C]: Contravariant[Exemplar[F, *, C]] =
        new Contravariant[Exemplar[F, *, C]] {
          override def contramap[A, B](fa: Exemplar[F, A, C])(f: B => A): Exemplar[F, B, C] = fa.contramap(f)
        }

      implicit def labelsContravariant[F[_], C]: LabelsContravariant[Exemplar[F, C, *]] =
        new LabelsContravariant[Exemplar[F, C, *]] {
          override def contramapLabels[A, B](fa: Exemplar[F, C, A])(f: B => A): Exemplar[F, C, B] =
            fa.contramapLabels(f)
        }

      def make[F[_]: FlatMap: prometheus4cats.Exemplar, A, B](
          _observe: (A, B, Option[prometheus4cats.Exemplar.Labels]) => F[Unit]
      ): Exemplar[F, A, B] =
        new Exemplar[F, A, B] {
          override def observe(n: A, labels: B): F[Unit] = _observe(n, labels, None)

          override def observeWithExemplar(n: A, labels: B): F[Unit] =
            prometheus4cats.Exemplar[F].get.flatMap(_observe(n, labels, _))

        }

      private[prometheus4cats] def fromHistogram[F[_], A, B](
          histogram: Histogram.Labelled[F, A, B]
      ): Exemplar[F, A, B] =
        new Exemplar[F, A, B] {
          override def observe(n: A, labels: B): F[Unit] = histogram.observe(n, labels)

          override def observeWithExemplar(n: A, labels: B): F[Unit] = histogram.observe(n, labels)
        }

      def noop[F[_]: Applicative, A, B]: Exemplar[F, A, B] =
        new Exemplar[F, A, B] {
          override def observe(n: A, labels: B): F[Unit] = Applicative[F].unit

          override def observeWithExemplar(n: A, labels: B): F[Unit] = Applicative[F].unit

        }
    }

  }
}
