package prometheus4cats.mapref

import cats.{Applicative, FlatMap, Functor}
import cats.data.NonEmptySeq
import cats.effect.MonadCancelThrow
import cats.effect.kernel.Concurrent
import cats.effect.std.Semaphore
import io.chrisdavenport.mapref.MapRef
import prometheus4cats.{Counter, Gauge, Histogram, Info, Label, Metric, MetricRegistry, Summary}

import scala.concurrent.duration.FiniteDuration
import cats.syntax.show._
import cats.syntax.flatMap._
import cats.syntax.functor._
import prometheus4cats.mapref.internal.MetricValues
import alleycats.std.iterable._
import cats.syntax.traverse._

case class Thing[F[_]: Concurrent, A, B <: internal.MetricValues[F]](
  zero: () => B,
  map: scala.collection.concurrent.Map[(Option[Metric.Prefix], A), B],
  sem: Semaphore[F]
) {
  private val metrics = MapRef.fromScalaConcurrentMap(map)

  def update(prefix: Option[Metric.Prefix], name: A)(f: B => F[Unit]): F[Unit] =
    sem.permit.surround {
      val ref = metrics(prefix -> name)

      ref.modify {
        case Some(value) =>
          (Some(value), f(value))
        case None =>
          val value = zero()
          (Some(value), f(value))
      }.flatten
    }

  val values: F[Iterable[MetricValues.Value]] = map.values.traverse(_.values).map(_.flatten)
}

class MapRefRegistry[F[_]: Concurrent](
  counters: Thing[F, Counter.Name, internal.MetricValues.CounterValues[F]],
  gauges: Thing[F, Gauge.Name, internal.MetricValues.GaugeValues[F]]
) extends MetricRegistry[F] {

  override protected[prometheus4cats] def createAndRegisterDoubleCounter(
    prefix: Option[Metric.Prefix],
    name: Counter.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels
  ): F[Counter[F, Double]] = ???

  override protected[prometheus4cats] def createAndRegisterLongCounter(
    prefix: Option[Metric.Prefix],
    name: Counter.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels
  ): F[Counter[F, Long]] = ???

  override protected[prometheus4cats] def createAndRegisterLabelledDoubleCounter[A](
    prefix: Option[Metric.Prefix],
    name: Counter.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels,
    labelNames: IndexedSeq[Label.Name]
  )(f: A => IndexedSeq[String]): F[Counter.Labelled[F, Double, A]] = {
    lazy val cls = commonLabels.value.toIndexedSeq

    Applicative[F].pure(Counter.Labelled.make[F, Double, A] { (d: Double, a: A) =>
      counters.update(prefix, name)(_.inc(labelNames.zip(f(a)) ++ cls, d))
    })
  }

  override protected[prometheus4cats] def createAndRegisterLabelledLongCounter[A](
    prefix: Option[Metric.Prefix],
    name: Counter.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels,
    labelNames: IndexedSeq[Label.Name]
  )(f: A => IndexedSeq[String]): F[Counter.Labelled[F, Long, A]] = ???

  override protected[prometheus4cats] def createAndRegisterDoubleGauge(
    prefix: Option[Metric.Prefix],
    name: Gauge.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels
  ): F[Gauge[F, Double]] = ???

  override protected[prometheus4cats] def createAndRegisterLongGauge(
    prefix: Option[Metric.Prefix],
    name: Gauge.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels
  ): F[Gauge[F, Long]] = ???

  override protected[prometheus4cats] def createAndRegisterLabelledDoubleGauge[A](
    prefix: Option[Metric.Prefix],
    name: Gauge.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels,
    labelNames: IndexedSeq[Label.Name]
  )(f: A => IndexedSeq[String]): F[Gauge.Labelled[F, Double, A]] = {
    lazy val cls = commonLabels.value.toIndexedSeq

    val inc: (Double, A) => F[Unit] = (d, a) =>
      gauges.update(prefix, name)(_.inc(labelNames.zip(f(a)) ++ cls, Right(d)))

    val dec: (Double, A) => F[Unit] = (d, a) =>
      gauges.update(prefix, name)(_.dec(labelNames.zip(f(a)) ++ cls, Right(d)))

    val set: (Double, A) => F[Unit] = (d, a) => {
      gauges.update(prefix, name)(_.set(labelNames.zip(f(a)) ++ cls, Right(d)))
    }

    Applicative[F].pure(Gauge.Labelled.make[F, Double, A](inc, dec, set))
  }

  override protected[prometheus4cats] def createAndRegisterLabelledLongGauge[A](
    prefix: Option[Metric.Prefix],
    name: Gauge.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels,
    labelNames: IndexedSeq[Label.Name]
  )(f: A => IndexedSeq[String]): F[Gauge.Labelled[F, Long, A]] = ???

  override protected[prometheus4cats] def createAndRegisterDoubleHistogram(
    prefix: Option[Metric.Prefix],
    name: Histogram.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels,
    buckets: NonEmptySeq[Double]
  ): F[Histogram[F, Double]] = ???

  override protected[prometheus4cats] def createAndRegisterLongHistogram(
    prefix: Option[Metric.Prefix],
    name: Histogram.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels,
    buckets: NonEmptySeq[Long]
  ): F[Histogram[F, Long]] = ???

  override protected[prometheus4cats] def createAndRegisterLabelledDoubleHistogram[A](
    prefix: Option[Metric.Prefix],
    name: Histogram.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels,
    labelNames: IndexedSeq[Label.Name],
    buckets: NonEmptySeq[Double]
  )(f: A => IndexedSeq[String]): F[Histogram.Labelled[F, Double, A]] = ???

  override protected[prometheus4cats] def createAndRegisterLabelledLongHistogram[A](
    prefix: Option[Metric.Prefix],
    name: Histogram.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels,
    labelNames: IndexedSeq[Label.Name],
    buckets: NonEmptySeq[Long]
  )(f: A => IndexedSeq[String]): F[Histogram.Labelled[F, Long, A]] = ???

  override protected[prometheus4cats] def createAndRegisterDoubleSummary(
    prefix: Option[Metric.Prefix],
    name: Summary.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels,
    quantiles: Seq[Summary.QuantileDefinition],
    maxAge: FiniteDuration,
    ageBuckets: Summary.AgeBuckets
  ): F[Summary[F, Double]] = ???

  override protected[prometheus4cats] def createAndRegisterLongSummary(
    prefix: Option[Metric.Prefix],
    name: Summary.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels,
    quantiles: Seq[Summary.QuantileDefinition],
    maxAge: FiniteDuration,
    ageBuckets: Summary.AgeBuckets
  ): F[Summary[F, Long]] = ???

  override protected[prometheus4cats] def createAndRegisterLabelledDoubleSummary[A](
    prefix: Option[Metric.Prefix],
    name: Summary.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels,
    labelNames: IndexedSeq[Label.Name],
    quantiles: Seq[Summary.QuantileDefinition],
    maxAge: FiniteDuration,
    ageBuckets: Summary.AgeBuckets
  )(f: A => IndexedSeq[String]): F[Summary.Labelled[F, Double, A]] = ???

  override protected[prometheus4cats] def createAndRegisterLabelledLongSummary[A](
    prefix: Option[Metric.Prefix],
    name: Summary.Name,
    help: Metric.Help,
    commonLabels: Metric.CommonLabels,
    labelNames: IndexedSeq[Label.Name],
    quantiles: Seq[Summary.QuantileDefinition],
    maxAge: FiniteDuration,
    ageBuckets: Summary.AgeBuckets
  )(f: A => IndexedSeq[String]): F[Summary.Labelled[F, Long, A]] = ???

  override protected[prometheus4cats] def createAndRegisterInfo(
    prefix: Option[Metric.Prefix],
    name: Info.Name,
    help: Metric.Help
  ): F[Info[F, Map[Label.Name, String]]] = ???
}
