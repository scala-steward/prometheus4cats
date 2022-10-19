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

package prometheus4cats.util

import cats.Functor
import cats.data.NonEmptySeq
import cats.syntax.functor._
import prometheus4cats._

trait DoubleCallbackRegistry[F[_]] extends CallbackRegistry[F] {
  implicit protected val F: Functor[F]

  override protected[prometheus4cats] def registerLongCounterCallback(
      prefix: Option[Metric.Prefix],
      name: Counter.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      callback: F[Long]
  ): F[Unit] = registerDoubleCounterCallback(prefix, name, help, commonLabels, callback.map(_.toDouble))

  override protected[prometheus4cats] def registerLabelledLongCounterCallback[A](
      prefix: Option[Metric.Prefix],
      name: Counter.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      labelNames: IndexedSeq[Label.Name],
      callback: F[(Long, A)]
  )(f: A => IndexedSeq[String]): F[Unit] = registerLabelledDoubleCounterCallback(
    prefix,
    name,
    help,
    commonLabels,
    labelNames,
    callback.map { case (v, a) => v.toDouble -> a }
  )(f)

  override protected[prometheus4cats] def registerLongGaugeCallback(
      prefix: Option[Metric.Prefix],
      name: Gauge.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      callback: F[Long]
  ): F[Unit] = registerDoubleGaugeCallback(prefix, name, help, commonLabels, callback.map(_.toDouble))

  override protected[prometheus4cats] def registerLabelledLongGaugeCallback[A](
      prefix: Option[Metric.Prefix],
      name: Gauge.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      labelNames: IndexedSeq[Label.Name],
      callback: F[(Long, A)]
  )(f: A => IndexedSeq[String]): F[Unit] = registerLabelledDoubleGaugeCallback(
    prefix,
    name,
    help,
    commonLabels,
    labelNames,
    callback.map { case (v, a) => v.toDouble -> a }
  )(f)

  override protected[prometheus4cats] def registerLongHistogramCallback(
      prefix: Option[Metric.Prefix],
      name: Histogram.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      buckets: NonEmptySeq[Long],
      callback: F[Histogram.Value[Long]]
  ): F[Unit] = registerDoubleHistogramCallback(
    prefix,
    name,
    help,
    commonLabels,
    buckets.map(_.toDouble),
    callback.map(_.map(_.toDouble))
  )

  override protected[prometheus4cats] def registerLabelledLongHistogramCallback[A](
      prefix: Option[Metric.Prefix],
      name: Histogram.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      labelNames: IndexedSeq[Label.Name],
      buckets: NonEmptySeq[Long],
      callback: F[(Histogram.Value[Long], A)]
  )(f: A => IndexedSeq[String]): F[Unit] = registerLabelledDoubleHistogramCallback(
    prefix,
    name,
    help,
    commonLabels,
    labelNames,
    buckets.map(_.toDouble),
    callback.map { case (v, a) => v.map(_.toDouble) -> a }
  )(f)
}
