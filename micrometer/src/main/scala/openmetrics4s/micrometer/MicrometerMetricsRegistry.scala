package openmetrics4s.micrometer

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import java.util.function.ToDoubleFunction

import cats.data.NonEmptySeq
import cats.effect.kernel.{Clock, Sync}
import io.micrometer.core.instrument.{MeterRegistry, Tag}
import openmetrics4s.{Counter, Gauge, Histogram, Label, Metric, MetricsRegistry}

import scala.jdk.CollectionConverters._
import cats.syntax.functor._
import cats.syntax.flatMap._

import scala.annotation.tailrec

class MicrometerMetricsRegistry[F[_]: Sync](registry: MeterRegistry) extends MetricsRegistry[F] {
  override def createAndRegisterCounter(
      prefix: Option[Metric.Prefix],
      name: Counter.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels
  ): F[Counter[F]] = Sync[F]
    .delay(
      registry.counter(name.value, commonLabels.value.map { case (name, value) => Tag.of(name.value, value) }.asJava)
    )
    .map { counter =>
      Counter.make(d => Sync[F].delay(counter.increment(d)))
    }

  override def createAndRegisterLabelledCounter[A](
      prefix: Option[Metric.Prefix],
      name: Counter.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      labelNames: IndexedSeq[Label.Name]
  )(f: A => IndexedSeq[String]): F[Counter.Labelled[F, A]] = ???

  private val toDoubleFunction = new ToDoubleFunction[AtomicReference[Double]] {
    override def applyAsDouble(value: AtomicReference[Double]): Double = value.get()
  }

  private def modify(ar: AtomicReference[Double])(f: Double => Double): F[Unit] = {
    @tailrec
    def spin(): Unit = {
      val c = ar.get
      val u = f(c)
      if (!ar.compareAndSet(c, u)) spin()
      else ()
    }

    Sync[F].delay(spin())
  }

  override def createAndRegisterGauge(
      prefix: Option[Metric.Prefix],
      name: Gauge.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels
  ): F[Gauge[F]] = Sync[F]
    .delay(
      registry.gauge(
        name.value,
        commonLabels.value.map { case (name, value) => Tag.of(name.value, value) }.asJava,
        new AtomicReference[Double](),
        toDoubleFunction
      )
    )
    .map { ref =>
      val mod = modify(ref)(_)

      Gauge.make(
        d => mod(_ + d),
        d => mod(_ - d),
        d => mod(_ => d),
        Clock[F].monotonic.flatMap(d => mod(_ => d.toSeconds.toDouble))
      )
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
