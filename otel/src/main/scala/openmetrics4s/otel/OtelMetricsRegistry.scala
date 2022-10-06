package openmetrics4s.otel

import cats.data.NonEmptySeq
import cats.effect.kernel.Sync
import cats.syntax.functor._
import cats.syntax.flatMap._
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.{Meter, ObservableDoubleMeasurement, ObservableMeasurement}
import openmetrics4s._
import openmetrics4s.util.NameUtils

class OtelMetricsRegistry[F[_]: Sync](meter: Meter) extends MetricsRegistry[F] {
  private def labelsToAttributes[A](commonLabels: Metric.CommonLabels, labels: IndexedSeq[Label.Name], labelValues: A)(
      f: A => IndexedSeq[String]
  ): Attributes = (commonLabels.value ++ labels.zip(f(labelValues)))
    .foldLeft(Attributes.builder()) { case (builder, (name, value)) =>
      builder.put(name.value, value)
    }
    .build()

  override def createAndRegisterCounter(
      prefix: Option[Metric.Prefix],
      name: Counter.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels
  ): F[Counter[F]] =
    Sync[F].delay(meter.counterBuilder(NameUtils.makeName(prefix, name)).ofDoubles().build()).map { counter =>
      Counter.make(d => Sync[F].delay(counter.add(d)))
    }

  override def createAndRegisterLabelledCounter[A](
      prefix: Option[Metric.Prefix],
      name: Counter.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      labelNames: IndexedSeq[Label.Name]
  )(f: A => IndexedSeq[String]): F[Counter.Labelled[F, A]] =
    Sync[F].delay(meter.counterBuilder(NameUtils.makeName(prefix, name)).ofDoubles().build()).map { counter =>
      Counter.Labelled.make((d, a) => Sync[F].delay(counter.add(d, labelsToAttributes(commonLabels, labelNames, a)(f))))
    }

  override def createAndRegisterGauge(
      prefix: Option[Metric.Prefix],
      name: Gauge.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels
  ): F[Gauge[F]] = {
    val f: Double => ObservableDoubleMeasurement => Unit = d => _.record(d)

    Sync[F].delay(meter.upDownCounterBuilder(NameUtils.makeName(prefix, name)).ofDoubles().buildWithCallback()).map {
      gauge =>
        Gauge.make()
    }
  }

  override def createAndRegisterLabelledGauge[A](
      prefix: Option[Metric.Prefix],
      name: Gauge.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      labelNames: IndexedSeq[Label.Name]
  )(f: A => IndexedSeq[String]): F[Gauge.Labelled[F, A]] = ???

  override def createAndRegisterHistogram(
      prefix: Option[Metric.Prefix],
      name: Histogram.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      buckets: NonEmptySeq[Double]
  ): F[Histogram[F]] = ???

  override def createAndRegisterLabelledHistogram[A](
      prefix: Option[Metric.Prefix],
      name: Histogram.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      labelNames: IndexedSeq[Label.Name],
      buckets: NonEmptySeq[Double]
  )(f: A => IndexedSeq[String]): F[Histogram.Labelled[F, A]] = ???
}
