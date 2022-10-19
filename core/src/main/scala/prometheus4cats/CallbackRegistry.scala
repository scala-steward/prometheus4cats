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
import cats.{Applicative, ~>}

/** Trait for registering callbacks against different backends. May be implemented by anyone for use with
  * [[MetricFactory.WithCallbacks]]
  */
trait CallbackRegistry[F[_]] {

  /** Register a counter value that records [[scala.Double]] values against a callback registry
    *
    * @param prefix
    *   optional [[Metric.Prefix]] to be prepended to the metric name
    * @param name
    *   [[Counter.Name]] metric name
    * @param help
    *   [[Metric.Help]] string to describe the metric
    * @param commonLabels
    *   [[Metric.CommonLabels]] map of common labels to be added to the metric
    * @param callback
    *   Some effectful operation that returns a [[scala.Double]]
    * @return
    *   An empty side effect to indicate that the callback has been registered
    */
  protected[prometheus4cats] def registerDoubleCounterCallback(
      prefix: Option[Metric.Prefix],
      name: Counter.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      callback: F[Double]
  ): F[Unit]

  /** Register a counter value that records [[scala.Long]] values against a callback registry
    *
    * @param prefix
    *   optional [[Metric.Prefix]] to be prepended to the metric name
    * @param name
    *   [[Counter.Name]] metric name
    * @param help
    *   [[Metric.Help]] string to describe the metric
    * @param commonLabels
    *   [[Metric.CommonLabels]] map of common labels to be added to the metric
    * @param callback
    *   Some effectful operation that returns a [[scala.Long]]
    * @return
    *   An empty side effect to indicate that the callback has been registered
    */
  protected[prometheus4cats] def registerLongCounterCallback(
      prefix: Option[Metric.Prefix],
      name: Counter.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      callback: F[Long]
  ): F[Unit]

  /** Register a labelled counter value that records [[scala.Double]] values against a metrics registry
    *
    * @param prefix
    *   optional [[Metric.Prefix]] to be prepended to the metric name
    * @param name
    *   [[Counter.Name]] metric name
    * @param help
    *   [[Metric.Help]] string to describe the metric
    * @param commonLabels
    *   [[Metric.CommonLabels]] map of common labels to be added to the metric
    * @param labelNames
    *   an [[scala.IndexedSeq]] of [[Label.Name]]s to annotate the metric with
    * @param f
    *   a function from `A` to an [[scala.IndexedSeq]] of [[java.lang.String]] that provides label values, which must be
    *   paired with their corresponding name in the [[scala.IndexedSeq]] of [[Label.Name]]s
    * @param callback
    *   Some effectful operation that returns a [[scala.Long]]
    * @return
    *   An empty side effect to indicate that the callback has been registered
    */
  protected[prometheus4cats] def registerLabelledDoubleCounterCallback[A](
      prefix: Option[Metric.Prefix],
      name: Counter.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      labelNames: IndexedSeq[Label.Name],
      callback: F[(Double, A)]
  )(f: A => IndexedSeq[String]): F[Unit]

  /** Register a labelled counter value that records [[scala.Long]] values against a metrics registry
    *
    * @param prefix
    *   optional [[Metric.Prefix]] to be prepended to the metric name
    * @param name
    *   [[Counter.Name]] metric name
    * @param help
    *   [[Metric.Help]] string to describe the metric
    * @param commonLabels
    *   [[Metric.CommonLabels]] map of common labels to be added to the metric
    * @param labelNames
    *   an [[scala.IndexedSeq]] of [[Label.Name]]s to annotate the metric with
    * @param f
    *   a function from `A` to an [[scala.IndexedSeq]] of [[java.lang.String]] that provides label values, which must be
    *   paired with their corresponding name in the [[scala.IndexedSeq]] of [[Label.Name]]s
    * @param callback
    *   Some effectful operation that returns a [[scala.Double]]
    * @return
    *   An empty side effect to indicate that the callback has been registered
    */
  protected[prometheus4cats] def registerLabelledLongCounterCallback[A](
      prefix: Option[Metric.Prefix],
      name: Counter.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      labelNames: IndexedSeq[Label.Name],
      callback: F[(Long, A)]
  )(f: A => IndexedSeq[String]): F[Unit]

  /** Register a gauge value that records [[scala.Double]] values against a callback registry
    *
    * @param prefix
    *   optional [[Metric.Prefix]] to be prepended to the metric name
    * @param name
    *   [[Counter.Name]] metric name
    * @param help
    *   [[Metric.Help]] string to describe the metric
    * @param commonLabels
    *   [[Metric.CommonLabels]] map of common labels to be added to the metric
    * @param callback
    *   Some effectful operation that returns a [[scala.Double]]
    * @return
    *   An empty side effect to indicate that the callback has been registered
    */
  protected[prometheus4cats] def registerDoubleGaugeCallback(
      prefix: Option[Metric.Prefix],
      name: Gauge.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      callback: F[Double]
  ): F[Unit]

  /** Register a gauge value that records [[scala.Long]] values against a callback registry
    *
    * @param prefix
    *   optional [[Metric.Prefix]] to be prepended to the metric name
    * @param name
    *   [[Counter.Name]] metric name
    * @param help
    *   [[Metric.Help]] string to describe the metric
    * @param commonLabels
    *   [[Metric.CommonLabels]] map of common labels to be added to the metric
    * @param callback
    *   Some effectful operation that returns a [[scala.Long]]
    * @return
    *   An empty side effect to indicate that the callback has been registered
    */
  protected[prometheus4cats] def registerLongGaugeCallback(
      prefix: Option[Metric.Prefix],
      name: Gauge.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      callback: F[Long]
  ): F[Unit]

  /** Register a labelled gauge value that records [[scala.Double]] values against a metrics registry
    *
    * @param prefix
    *   optional [[Metric.Prefix]] to be prepended to the metric name
    * @param name
    *   [[Counter.Name]] metric name
    * @param help
    *   [[Metric.Help]] string to describe the metric
    * @param commonLabels
    *   [[Metric.CommonLabels]] map of common labels to be added to the metric
    * @param labelNames
    *   an [[scala.IndexedSeq]] of [[Label.Name]]s to annotate the metric with
    * @param f
    *   a function from `A` to an [[scala.IndexedSeq]] of [[java.lang.String]] that provides label values, which must be
    *   paired with their corresponding name in the [[scala.IndexedSeq]] of [[Label.Name]]s
    * @param callback
    *   Some effectful operation that returns a [[scala.Double]]
    * @return
    *   An empty side effect to indicate that the callback has been registered
    */
  protected[prometheus4cats] def registerLabelledDoubleGaugeCallback[A](
      prefix: Option[Metric.Prefix],
      name: Gauge.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      labelNames: IndexedSeq[Label.Name],
      callback: F[(Double, A)]
  )(f: A => IndexedSeq[String]): F[Unit]

  /** Register a labelled gauge value that records [[scala.Long]] values against a metrics registry
    *
    * @param prefix
    *   optional [[Metric.Prefix]] to be prepended to the metric name
    * @param name
    *   [[Counter.Name]] metric name
    * @param help
    *   [[Metric.Help]] string to describe the metric
    * @param commonLabels
    *   [[Metric.CommonLabels]] map of common labels to be added to the metric
    * @param labelNames
    *   an [[scala.IndexedSeq]] of [[Label.Name]]s to annotate the metric with
    * @param f
    *   a function from `A` to an [[scala.IndexedSeq]] of [[java.lang.String]] that provides label values, which must be
    *   paired with their corresponding name in the [[scala.IndexedSeq]] of [[Label.Name]]s
    * @param callback
    *   Some effectful operation that returns a [[scala.Long]]
    * @return
    *   An empty side effect to indicate that the callback has been registered
    */
  protected[prometheus4cats] def registerLabelledLongGaugeCallback[A](
      prefix: Option[Metric.Prefix],
      name: Gauge.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      labelNames: IndexedSeq[Label.Name],
      callback: F[(Long, A)]
  )(f: A => IndexedSeq[String]): F[Unit]

  /** Register a histogram value that records [[scala.Double]] values against a callback registry
    *
    * @param prefix
    *   optional [[Metric.Prefix]] to be prepended to the metric name
    * @param name
    *   [[Counter.Name]] metric name
    * @param help
    *   [[Metric.Help]] string to describe the metric
    * @param commonLabels
    *   [[Metric.CommonLabels]] map of common labels to be added to the metric
    * @param callback
    *   Some effectful operation that returns a [[Histogram.Value]] parameterised with [[scala.Double]]
    * @return
    *   An empty side effect to indicate that the callback has been registered
    */
  protected[prometheus4cats] def registerDoubleHistogramCallback(
      prefix: Option[Metric.Prefix],
      name: Histogram.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      buckets: NonEmptySeq[Double],
      callback: F[Histogram.Value[Double]]
  ): F[Unit]

  /** Register a histogram value that records [[scala.Long]] values against a callback registry
    *
    * @param prefix
    *   optional [[Metric.Prefix]] to be prepended to the metric name
    * @param name
    *   [[Counter.Name]] metric name
    * @param help
    *   [[Metric.Help]] string to describe the metric
    * @param commonLabels
    *   [[Metric.CommonLabels]] map of common labels to be added to the metric
    * @param callback
    *   Some effectful operation that returns a [[Histogram.Value]] parameterised with [[scala.Long]]
    * @return
    *   An empty side effect to indicate that the callback has been registered
    */
  protected[prometheus4cats] def registerLongHistogramCallback(
      prefix: Option[Metric.Prefix],
      name: Histogram.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      buckets: NonEmptySeq[Long],
      callback: F[Histogram.Value[Long]]
  ): F[Unit]

  /** Register a labelled histogram value that records [[scala.Double]] values against a metrics registry
    *
    * @param prefix
    *   optional [[Metric.Prefix]] to be prepended to the metric name
    * @param name
    *   [[Counter.Name]] metric name
    * @param help
    *   [[Metric.Help]] string to describe the metric
    * @param commonLabels
    *   [[Metric.CommonLabels]] map of common labels to be added to the metric
    * @param labelNames
    *   an [[scala.IndexedSeq]] of [[Label.Name]]s to annotate the metric with
    * @param f
    *   a function from `A` to an [[scala.IndexedSeq]] of [[java.lang.String]] that provides label values, which must be
    *   paired with their corresponding name in the [[scala.IndexedSeq]] of [[Label.Name]]s
    * @param callback
    *   Some effectful operation that returns a [[Histogram.Value]] parameterised with [[scala.Double]]
    * @return
    *   An empty side effect to indicate that the callback has been registered
    */
  protected[prometheus4cats] def registerLabelledDoubleHistogramCallback[A](
      prefix: Option[Metric.Prefix],
      name: Histogram.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      labelNames: IndexedSeq[Label.Name],
      buckets: NonEmptySeq[Double],
      callback: F[(Histogram.Value[Double], A)]
  )(f: A => IndexedSeq[String]): F[Unit]

  /** Register a labelled histogram value that records [[scala.Long]] values against a metrics registry
    *
    * @param prefix
    *   optional [[Metric.Prefix]] to be prepended to the metric name
    * @param name
    *   [[Counter.Name]] metric name
    * @param help
    *   [[Metric.Help]] string to describe the metric
    * @param commonLabels
    *   [[Metric.CommonLabels]] map of common labels to be added to the metric
    * @param labelNames
    *   an [[scala.IndexedSeq]] of [[Label.Name]]s to annotate the metric with
    * @param f
    *   a function from `A` to an [[scala.IndexedSeq]] of [[java.lang.String]] that provides label values, which must be
    *   paired with their corresponding name in the [[scala.IndexedSeq]] of [[Label.Name]]s
    * @param callback
    *   Some effectful operation that returns a [[Histogram.Value]] parameterised with [[scala.Long]]
    * @return
    *   An empty side effect to indicate that the callback has been registered
    */
  protected[prometheus4cats] def registerLabelledLongHistogramCallback[A](
      prefix: Option[Metric.Prefix],
      name: Histogram.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      labelNames: IndexedSeq[Label.Name],
      buckets: NonEmptySeq[Long],
      callback: F[(Histogram.Value[Long], A)]
  )(f: A => IndexedSeq[String]): F[Unit]

  /** Given a natural transformation from `F` to `G` and from `G` to `F`, transforms this [[CallbackRegistry]] from
    * effect `F` to effect `G`
    */
  final def imapK[G[_]](fk: F ~> G, gk: G ~> F): CallbackRegistry[G] = CallbackRegistry.imapK(this, fk, gk)
}

object CallbackRegistry {
  def noop[F[_]](implicit F: Applicative[F]): CallbackRegistry[F] = new CallbackRegistry[F] {
    override protected[prometheus4cats] def registerDoubleCounterCallback(
        prefix: Option[Metric.Prefix],
        name: Counter.Name,
        help: Metric.Help,
        commonLabels: Metric.CommonLabels,
        callback: F[Double]
    ): F[Unit] = F.unit

    override protected[prometheus4cats] def registerLongCounterCallback(
        prefix: Option[Metric.Prefix],
        name: Counter.Name,
        help: Metric.Help,
        commonLabels: Metric.CommonLabels,
        callback: F[Long]
    ): F[Unit] = F.unit

    override protected[prometheus4cats] def registerLabelledDoubleCounterCallback[A](
        prefix: Option[Metric.Prefix],
        name: Counter.Name,
        help: Metric.Help,
        commonLabels: Metric.CommonLabels,
        labelNames: IndexedSeq[Label.Name],
        callback: F[(Double, A)]
    )(f: A => IndexedSeq[String]): F[Unit] = F.unit

    override protected[prometheus4cats] def registerLabelledLongCounterCallback[A](
        prefix: Option[Metric.Prefix],
        name: Counter.Name,
        help: Metric.Help,
        commonLabels: Metric.CommonLabels,
        labelNames: IndexedSeq[Label.Name],
        callback: F[(Long, A)]
    )(f: A => IndexedSeq[String]): F[Unit] = F.unit

    override protected[prometheus4cats] def registerDoubleGaugeCallback(
        prefix: Option[Metric.Prefix],
        name: Gauge.Name,
        help: Metric.Help,
        commonLabels: Metric.CommonLabels,
        callback: F[Double]
    ): F[Unit] = F.unit

    override protected[prometheus4cats] def registerLongGaugeCallback(
        prefix: Option[Metric.Prefix],
        name: Gauge.Name,
        help: Metric.Help,
        commonLabels: Metric.CommonLabels,
        callback: F[Long]
    ): F[Unit] = F.unit

    override protected[prometheus4cats] def registerLabelledDoubleGaugeCallback[A](
        prefix: Option[Metric.Prefix],
        name: Gauge.Name,
        help: Metric.Help,
        commonLabels: Metric.CommonLabels,
        labelNames: IndexedSeq[Label.Name],
        callback: F[(Double, A)]
    )(f: A => IndexedSeq[String]): F[Unit] = F.unit

    override protected[prometheus4cats] def registerLabelledLongGaugeCallback[A](
        prefix: Option[Metric.Prefix],
        name: Gauge.Name,
        help: Metric.Help,
        commonLabels: Metric.CommonLabels,
        labelNames: IndexedSeq[Label.Name],
        callback: F[(Long, A)]
    )(f: A => IndexedSeq[String]): F[Unit] = F.unit

    override protected[prometheus4cats] def registerDoubleHistogramCallback(
        prefix: Option[Metric.Prefix],
        name: Histogram.Name,
        help: Metric.Help,
        commonLabels: Metric.CommonLabels,
        buckets: NonEmptySeq[Double],
        callback: F[Histogram.Value[Double]]
    ): F[Unit] = F.unit

    override protected[prometheus4cats] def registerLongHistogramCallback(
        prefix: Option[Metric.Prefix],
        name: Histogram.Name,
        help: Metric.Help,
        commonLabels: Metric.CommonLabels,
        buckets: NonEmptySeq[Long],
        callback: F[Histogram.Value[Long]]
    ): F[Unit] = F.unit

    override protected[prometheus4cats] def registerLabelledDoubleHistogramCallback[A](
        prefix: Option[Metric.Prefix],
        name: Histogram.Name,
        help: Metric.Help,
        commonLabels: Metric.CommonLabels,
        labelNames: IndexedSeq[Label.Name],
        buckets: NonEmptySeq[Double],
        callback: F[(Histogram.Value[Double], A)]
    )(f: A => IndexedSeq[String]): F[Unit] = F.unit

    override protected[prometheus4cats] def registerLabelledLongHistogramCallback[A](
        prefix: Option[Metric.Prefix],
        name: Histogram.Name,
        help: Metric.Help,
        commonLabels: Metric.CommonLabels,
        labelNames: IndexedSeq[Label.Name],
        buckets: NonEmptySeq[Long],
        callback: F[(Histogram.Value[Long], A)]
    )(f: A => IndexedSeq[String]): F[Unit] = F.unit
  }

  def imapK[F[_], G[_]](self: CallbackRegistry[F], fk: F ~> G, gk: G ~> F): CallbackRegistry[G] =
    new CallbackRegistry[G] {
      override protected[prometheus4cats] def registerDoubleCounterCallback(
          prefix: Option[Metric.Prefix],
          name: Counter.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          callback: G[Double]
      ): G[Unit] = fk(self.registerDoubleCounterCallback(prefix, name, help, commonLabels, gk(callback)))

      override protected[prometheus4cats] def registerLongCounterCallback(
          prefix: Option[Metric.Prefix],
          name: Counter.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          callback: G[Long]
      ): G[Unit] = fk(self.registerLongCounterCallback(prefix, name, help, commonLabels, gk(callback)))

      override protected[prometheus4cats] def registerLabelledDoubleCounterCallback[A](
          prefix: Option[Metric.Prefix],
          name: Counter.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          labelNames: IndexedSeq[Label.Name],
          callback: G[(Double, A)]
      )(f: A => IndexedSeq[String]): G[Unit] = fk(
        self.registerLabelledDoubleCounterCallback(prefix, name, help, commonLabels, labelNames, gk(callback))(f)
      )

      override protected[prometheus4cats] def registerLabelledLongCounterCallback[A](
          prefix: Option[Metric.Prefix],
          name: Counter.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          labelNames: IndexedSeq[Label.Name],
          callback: G[(Long, A)]
      )(f: A => IndexedSeq[String]): G[Unit] = fk(
        self.registerLabelledLongCounterCallback(prefix, name, help, commonLabels, labelNames, gk(callback))(f)
      )

      override protected[prometheus4cats] def registerDoubleGaugeCallback(
          prefix: Option[Metric.Prefix],
          name: Gauge.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          callback: G[Double]
      ): G[Unit] = fk(self.registerDoubleGaugeCallback(prefix, name, help, commonLabels, gk(callback)))

      override protected[prometheus4cats] def registerLongGaugeCallback(
          prefix: Option[Metric.Prefix],
          name: Gauge.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          callback: G[Long]
      ): G[Unit] = fk(self.registerLongGaugeCallback(prefix, name, help, commonLabels, gk(callback)))

      override protected[prometheus4cats] def registerLabelledDoubleGaugeCallback[A](
          prefix: Option[Metric.Prefix],
          name: Gauge.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          labelNames: IndexedSeq[Label.Name],
          callback: G[(Double, A)]
      )(f: A => IndexedSeq[String]): G[Unit] = fk(
        self.registerLabelledDoubleGaugeCallback(prefix, name, help, commonLabels, labelNames, gk(callback))(f)
      )

      override protected[prometheus4cats] def registerLabelledLongGaugeCallback[A](
          prefix: Option[Metric.Prefix],
          name: Gauge.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          labelNames: IndexedSeq[Label.Name],
          callback: G[(Long, A)]
      )(f: A => IndexedSeq[String]): G[Unit] = fk(
        self.registerLabelledLongGaugeCallback(prefix, name, help, commonLabels, labelNames, gk(callback))(f)
      )

      override protected[prometheus4cats] def registerDoubleHistogramCallback(
          prefix: Option[Metric.Prefix],
          name: Histogram.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          buckets: NonEmptySeq[Double],
          callback: G[Histogram.Value[Double]]
      ): G[Unit] = fk(self.registerDoubleHistogramCallback(prefix, name, help, commonLabels, buckets, gk(callback)))

      override protected[prometheus4cats] def registerLongHistogramCallback(
          prefix: Option[Metric.Prefix],
          name: Histogram.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          buckets: NonEmptySeq[Long],
          callback: G[Histogram.Value[Long]]
      ): G[Unit] = fk(self.registerLongHistogramCallback(prefix, name, help, commonLabels, buckets, gk(callback)))

      override protected[prometheus4cats] def registerLabelledDoubleHistogramCallback[A](
          prefix: Option[Metric.Prefix],
          name: Histogram.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          labelNames: IndexedSeq[Label.Name],
          buckets: NonEmptySeq[Double],
          callback: G[(Histogram.Value[Double], A)]
      )(f: A => IndexedSeq[String]): G[Unit] = fk(
        self.registerLabelledDoubleHistogramCallback(
          prefix,
          name,
          help,
          commonLabels,
          labelNames,
          buckets,
          gk(callback)
        )(f)
      )

      override protected[prometheus4cats] def registerLabelledLongHistogramCallback[A](
          prefix: Option[Metric.Prefix],
          name: Histogram.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          labelNames: IndexedSeq[Label.Name],
          buckets: NonEmptySeq[Long],
          callback: G[(Histogram.Value[Long], A)]
      )(f: A => IndexedSeq[String]): G[Unit] = fk(
        self.registerLabelledLongHistogramCallback(
          prefix,
          name,
          help,
          commonLabels,
          labelNames,
          buckets,
          gk(callback)
        )(f)
      )
    }
}
