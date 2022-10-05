package com.permutive.metrics.prometheus

import cats.Show
import cats.data.NonEmptySeq
import cats.effect.IO
import cats.effect.kernel.Resource
import com.permutive.metrics._
import com.permutive.metrics.testkit.MetricsRegistrySuite
import com.permutive.metrics.util.NameUtils
import io.prometheus.client.CollectorRegistry
import munit.ScalaCheckSuite
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import org.scalacheck.Prop._
import cats.syntax.either._

import scala.jdk.CollectionConverters._

class PrometheusMetricsRegistrySuite extends ScalaCheckSuite with MetricsRegistrySuite[CollectorRegistry] {
  implicit val logger: Logger[IO] = NoOpLogger.impl

  override val stateResource: Resource[IO, CollectorRegistry] = Resource.eval(IO.delay(new CollectorRegistry()))

  override def registryResource(state: CollectorRegistry): Resource[IO, MetricsRegistry[IO]] =
    PrometheusMetricsRegistry.fromSimpleClientRegistry[IO](state)

  def getMetricValue[A: Show](
      state: CollectorRegistry,
      prefix: Option[Metric.Prefix],
      name: A,
      commonLabels: Metric.CommonLabels,
      extraLabels: Map[Label.Name, String]
  ): Option[Double] = {
    val n = NameUtils.makeName(prefix, name)

    val allLabels = (commonLabels.value ++ extraLabels).map { case (n, v) => n.value -> v }

    // the prometheus collector registry returns 0.0 when calling `getSampleValue` even if the metric is missing,
    // despite what their Javadoc says
    state
      .metricFamilySamples()
      .asScala
      .toList
      .flatMap(_.samples.asScala.toList)
      .find { sample =>
        val labels = sample.labelNames.asScala.zip(sample.labelValues.asScala).toMap

        sample.name == n && labels == allLabels
      }
      .map(_.value)
  }

  override def getCounterValue(
      state: CollectorRegistry,
      prefix: Option[Metric.Prefix],
      name: Counter.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      extraLabels: Map[Label.Name, String]
  ): IO[Option[Double]] = IO(getMetricValue(state, prefix, name, commonLabels, extraLabels))

  override def getGaugeValue(
      state: CollectorRegistry,
      prefix: Option[Metric.Prefix],
      name: Gauge.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      extraLabels: Map[Label.Name, String]
  ): IO[Option[Double]] = IO(getMetricValue(state, prefix, name, commonLabels, extraLabels))

  override def getHistogramValue(
      state: CollectorRegistry,
      prefix: Option[Metric.Prefix],
      name: Histogram.Name,
      help: Metric.Help,
      commonLabels: Metric.CommonLabels,
      buckets: NonEmptySeq[Double],
      extraLabels: Map[Label.Name, String]
  ): IO[Option[Map[String, Double]]] =
    IO {
      val n = NameUtils.makeName(prefix, name)

      val allLabels = (commonLabels.value ++ extraLabels).map { case (n, v) => n.value -> v }

      // the prometheus collector registry returns 0.0 when calling `getSampleValue` even if the metric is missing,
      // despite what their Javadoc says
      state
        .metricFamilySamples()
        .asScala
        .find(_.name == n)
        .map { sample =>
          sample.samples.asScala.filter { sample =>
            val labels = sample.labelNames.asScala.zip(sample.labelValues.asScala).toMap

            labels - "le" == allLabels && labels.contains("le")
          }.map { sample =>
            (sample.labelValues.asScala.last, sample.value)
          }.toMap
        }
    }

  property("returns an existing metric when labels and name are the same") {
    forAll {
      (
          prefix: Option[Metric.Prefix],
          name: Counter.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          labels: Set[Label.Name]
      ) =>
        exec(stateResource.flatMap(registryResource).use { reg =>
          val metric = reg
            .createAndRegisterLabelledCounter[Map[Label.Name, String]](
              prefix,
              name,
              help,
              commonLabels,
              labels.toIndexedSeq
            )(_.values.toIndexedSeq)

          for {
            _ <- metric
            _ <- metric
          } yield ()
        })
    }
  }

  property("fails when a metric with the same name and different labels") {
    forAll {
      (
          prefix: Option[Metric.Prefix],
          name: Counter.Name,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          labels: Set[Label.Name],
          labelName2: Label.Name
      ) =>
        val res = exec(
          stateResource
            .flatMap(registryResource)
            .use { reg =>
              for {
                _ <- reg
                  .createAndRegisterLabelledCounter[Map[Label.Name, String]](
                    prefix,
                    name,
                    help,
                    commonLabels,
                    labels.toIndexedSeq
                  )(_.values.toIndexedSeq)
                _ <- reg
                  .createAndRegisterLabelledCounter[Map[Label.Name, String]](
                    prefix,
                    name,
                    help,
                    commonLabels,
                    IndexedSeq(labelName2)
                  )(_.values.toIndexedSeq)
              } yield ()
            }
            .attempt
        )

        assertEquals(
          res.leftMap(_.getMessage),
          Left(
            s"A metric with the same name as '${NameUtils.makeName(prefix, name)}' is already registered with different labels and/or type"
          )
        )
    }
  }

  property("fails when a metric with the same name and different type") {
    forAll {
      (
          prefix: Option[Metric.Prefix],
          name: Metric.Prefix,
          help: Metric.Help,
          commonLabels: Metric.CommonLabels,
          labels: Set[Label.Name]
      ) =>
        val counterName = Counter.Name.from(s"${name.value}_total").toOption.get
        val gaugeName = Gauge.Name.from(name.value).toOption.get

        val res = exec(
          stateResource
            .flatMap(registryResource)
            .use { reg =>
              for {
                _ <- reg
                  .createAndRegisterLabelledCounter[Map[Label.Name, String]](
                    prefix,
                    counterName,
                    help,
                    commonLabels,
                    labels.toIndexedSeq
                  )(_.values.toIndexedSeq)
                _ <- reg
                  .createAndRegisterLabelledGauge[Map[Label.Name, String]](
                    prefix,
                    gaugeName,
                    help,
                    commonLabels,
                    labels.toIndexedSeq
                  )(_.values.toIndexedSeq)
              } yield ()
            }
            .attempt
        )

        assertEquals(
          res.leftMap(_.getMessage),
          Left(
            s"A metric with the same name as '${NameUtils.makeName(prefix, gaugeName)}' is already registered with different labels and/or type"
          )
        )
    }
  }
}
