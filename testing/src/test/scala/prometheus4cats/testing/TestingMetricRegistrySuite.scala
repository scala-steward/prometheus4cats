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

package prometheus4cats.testing

import cats.data.{Chain, NonEmptySeq}
import cats.effect._
import munit.CatsEffectSuite
import prometheus4cats._
import scala.concurrent.duration._

class TestingMetricRegistrySuite extends CatsEffectSuite {

  suite[Counter[IO, Double]]("Counter")(
    (reg: TestingMetricRegistry[IO], prefix: Option[Metric.Prefix], name: String, commonLabels: Metric.CommonLabels) =>
      reg.createAndRegisterDoubleCounter(prefix, Counter.Name.unsafeFrom(name), Metric.Help("help"), commonLabels),
    (c: Counter[IO, Double], _: Metric.CommonLabels) => (c.inc >> c.inc(2.0), Chain(0.0, 1.0, 3.0)),
    (reg: TestingMetricRegistry[IO], prefix: Option[Metric.Prefix], name: String, commonLabels: Metric.CommonLabels) =>
      reg.counterHistory(prefix, Counter.Name.unsafeFrom(name), commonLabels)
  )

  labelledSuite[Counter.Labelled[IO, Double, IndexedSeq[String]]]("Counter")(
    (
        reg: TestingMetricRegistry[IO],
        prefix: Option[Metric.Prefix],
        name: String,
        commonLabels: Metric.CommonLabels,
        labels: IndexedSeq[Label.Name]
    ) =>
      reg.createAndRegisterLabelledDoubleCounter(
        prefix,
        Counter.Name.unsafeFrom(name),
        Metric.Help("help"),
        commonLabels,
        labels
      )(identity[IndexedSeq[String]]),
    (
        c: Counter.Labelled[IO, Double, IndexedSeq[String]],
        _: Metric.CommonLabels,
        labels: IndexedSeq[String]
    ) => (c.inc(labels) >> c.inc(2.0, labels), Chain(0.0, 1.0, 3.0)),
    (
        reg: TestingMetricRegistry[IO],
        prefix: Option[Metric.Prefix],
        name: String,
        commonLabels: Metric.CommonLabels,
        labelNames: IndexedSeq[Label.Name],
        labelValues: IndexedSeq[String]
    ) => reg.counterHistory(prefix, Counter.Name.unsafeFrom(name), commonLabels, labelNames, labelValues)
  )

  suite[Gauge[IO, Double]]("Gauge")(
    (reg: TestingMetricRegistry[IO], prefix: Option[Metric.Prefix], name: String, commonLabels: Metric.CommonLabels) =>
      reg.createAndRegisterDoubleGauge(prefix, Gauge.Name.unsafeFrom(name), Metric.Help("help"), commonLabels),
    (g: Gauge[IO, Double], _: Metric.CommonLabels) =>
      (g.inc >> g.dec >> g.inc(2.0) >> g.dec(2.0) >> g.set(-1.0) >> g.reset, Chain(0.0, 1.0, 0.0, 2.0, 0.0, -1.0, 0.0)),
    (reg: TestingMetricRegistry[IO], prefix: Option[Metric.Prefix], name: String, commonLabels: Metric.CommonLabels) =>
      reg.gaugeHistory(prefix, Gauge.Name.unsafeFrom(name), commonLabels)
  )

  labelledSuite[Gauge.Labelled[IO, Double, IndexedSeq[String]]]("Gauge")(
    (
        reg: TestingMetricRegistry[IO],
        prefix: Option[Metric.Prefix],
        name: String,
        commonLabels: Metric.CommonLabels,
        labels: IndexedSeq[Label.Name]
    ) =>
      reg.createAndRegisterLabelledDoubleGauge(
        prefix,
        Gauge.Name.unsafeFrom(name),
        Metric.Help("help"),
        commonLabels,
        labels
      )(identity[IndexedSeq[String]]),
    (
        g: Gauge.Labelled[IO, Double, IndexedSeq[String]],
        _: Metric.CommonLabels,
        labels: IndexedSeq[String]
    ) =>
      (
        g.inc(labels) >> g.dec(labels) >> g.inc(2.0, labels) >> g.dec(2.0, labels) >> g.set(-1.0, labels) >> g.reset(
          labels
        ),
        Chain(0.0, 1.0, 0.0, 2.0, 0.0, -1.0, 0.0)
      ),
    (
        reg: TestingMetricRegistry[IO],
        prefix: Option[Metric.Prefix],
        name: String,
        commonLabels: Metric.CommonLabels,
        labelNames: IndexedSeq[Label.Name],
        labelValues: IndexedSeq[String]
    ) => reg.gaugeHistory(prefix, Gauge.Name.unsafeFrom(name), commonLabels, labelNames, labelValues)
  )

  def suite[M <: Metric[Double]](name: String)(
      create: (TestingMetricRegistry[IO], Option[Metric.Prefix], String, Metric.CommonLabels) => Resource[IO, M],
      use: (
          M,
          Metric.CommonLabels
      ) => (IO[Unit], Chain[Double]),
      extract: (
          TestingMetricRegistry[IO],
          Option[Metric.Prefix],
          String,
          Metric.CommonLabels
      ) => IO[Option[Chain[Double]]]
  )(implicit loc: munit.Location): Unit = {
    test(s"$name - history") {
      TestingMetricRegistry[IO].flatMap { reg =>
        create(reg, None, "test_total", Metric.CommonLabels.empty).use { c =>
          val (run, expected) = use(c, Metric.CommonLabels.empty)
          run >> extract(reg, None, "test_total", Metric.CommonLabels.empty).assertEquals(Some(expected))
        }
      }
    }

    test(s"$name - prefixed history") {
      TestingMetricRegistry[IO].flatMap { reg =>
        create(reg, Some(Metric.Prefix("permutive")), "test_total", Metric.CommonLabels.empty).use { c =>
          val (run, expected) = use(c, Metric.CommonLabels.empty)
          run >> extract(reg, Some(Metric.Prefix("permutive")), "test_total", Metric.CommonLabels.empty)
            .assertEquals(Some(expected))
        }
      }
    }

    test(s"$name - concurrent resource lifecycle") {
      TestingMetricRegistry[IO].flatMap { reg =>
        IO.deferred[Unit].flatMap { wait =>
          val m = create(reg, None, "test_total", Metric.CommonLabels.empty)
          m.use { c =>
            val (run, expected) = use(c, Metric.CommonLabels.empty)
            // Wait for other fiber to use and release counter
            wait.get >>
              // metric should still be valid as we still have a reference to it
              run >> extract(reg, None, "test_total", Metric.CommonLabels.empty).assertEquals(Some(expected))
          }.start.flatMap { fiber =>
            m.use_ >> wait.complete(()) >> fiber.joinWithNever
          }
        } >> extract(reg, None, "test_total", Metric.CommonLabels.empty)
          // All scopes which reference metric have closed so it should be removed from registry
          .assertEquals(None)
      }

    }

    test(s"$name - nested resource lifecycle") {
      TestingMetricRegistry[IO].flatMap { reg =>
        val m = create(reg, None, "test_total", Metric.CommonLabels.empty)
        m.use { c =>
          val (run, expected) = use(c, Metric.CommonLabels.empty)
          // Metric should still be valid as we still have a reference to it
          run >>
            m.use_ >>
            extract(reg, None, "test_total", Metric.CommonLabels.empty).assertEquals(Some(expected))
        }
      }
    }

  }

  def labelledSuite[M <: Metric[Double] with Metric.Labelled[IndexedSeq[String]]](name: String)(
      create: (
          TestingMetricRegistry[IO],
          Option[Metric.Prefix],
          String,
          Metric.CommonLabels,
          IndexedSeq[Label.Name]
      ) => Resource[IO, M],
      use: (M, Metric.CommonLabels, IndexedSeq[String]) => (
          IO[Unit],
          Chain[Double]
      ),
      extract: (
          TestingMetricRegistry[IO],
          Option[Metric.Prefix],
          String,
          Metric.CommonLabels,
          IndexedSeq[Label.Name],
          IndexedSeq[String]
      ) => IO[Option[Chain[Double]]]
  )(implicit loc: munit.Location): Unit =
    test(s"$name - labelled history") {
      TestingMetricRegistry[IO].flatMap { reg =>
        create(reg, None, "test_total", Metric.CommonLabels.empty, IndexedSeq(Label.Name("status"))).use { c =>
          val (run1, expected1) = use(c, Metric.CommonLabels.empty, IndexedSeq("success"))
          val (run2, expected2) = use(c, Metric.CommonLabels.empty, IndexedSeq("failure"))
          run1 >> run2 >> extract(
            reg,
            None,
            "test_total",
            Metric.CommonLabels.empty,
            IndexedSeq(Label.Name("status")),
            IndexedSeq("success")
          )
            .assertEquals(Some(expected1)) >> extract(
            reg,
            None,
            "test_total",
            Metric.CommonLabels.empty,
            IndexedSeq(Label.Name("status")),
            IndexedSeq("failure")
          )
            .assertEquals(Some(expected2))

        }
      }
    }

  test("Histogram - history") {
    TestingMetricRegistry[IO].flatMap { reg =>
      reg
        .createAndRegisterDoubleHistogram(
          None,
          Histogram.Name("test_total"),
          Metric.Help("help"),
          Metric.CommonLabels.empty,
          NonEmptySeq(0.0, Seq(5.0, 10.0))
        )
        .use { h =>
          h.observe(1.0) >> h.observe(2.0) >> h.observe(3.0) >>
            reg
              .histogramHistory(Histogram.Name("test_total"), Metric.CommonLabels.empty)
              .assertEquals(Some(Chain(1.0, 2.0, 3.0)))
        }
    }
  }

  test("Histogram - prefixed history") {
    TestingMetricRegistry[IO].flatMap { reg =>
      reg
        .createAndRegisterDoubleHistogram(
          Some(Metric.Prefix("permutive")),
          Histogram.Name("test_total"),
          Metric.Help("help"),
          Metric.CommonLabels.empty,
          NonEmptySeq(0.0, Seq(5.0, 10.0))
        )
        .use { h =>
          h.observe(1.0) >> h.observe(2.0) >> h.observe(3.0) >>
            reg
              .histogramHistory(
                Some(Metric.Prefix("permutive")),
                Histogram.Name("test_total"),
                Metric.CommonLabels.empty
              )
              .assertEquals(Some(Chain(1.0, 2.0, 3.0)))
        }
    }
  }

  test("Histogram - labelled history") {
    TestingMetricRegistry[IO].flatMap { reg =>
      reg
        .createAndRegisterLabelledDoubleHistogram(
          None,
          Histogram.Name("test_total"),
          Metric.Help("help"),
          Metric.CommonLabels.empty,
          IndexedSeq(Label.Name("status")),
          NonEmptySeq(0.0, Seq(5.0, 10.0))
        )((s: String) => IndexedSeq(s))
        .use { case h =>
          h.observe(1.0, "success") >> h.observe(2.0, "failure") >> reg
            .histogramHistory(
              Histogram.Name("test_total"),
              Metric.CommonLabels.empty,
              IndexedSeq(Label.Name("status")),
              IndexedSeq("success")
            )
            .assertEquals(Some(Chain(1.0))) >> reg
            .histogramHistory(
              Histogram.Name("test_total"),
              Metric.CommonLabels.empty,
              IndexedSeq(Label.Name("status")),
              IndexedSeq("failure")
            )
            .assertEquals(Some(Chain(2.0)))

        }
    }
  }

  test("Summary - history") {
    TestingMetricRegistry[IO].flatMap { reg =>
      reg
        .createAndRegisterDoubleSummary(
          None,
          Summary.Name("test_total"),
          Metric.Help("help"),
          Metric.CommonLabels.empty,
          Seq(Summary.QuantileDefinition(Summary.Quantile(0.5), Summary.AllowedError(0.1))),
          5.seconds,
          Summary.AgeBuckets(5)
        )
        .use { s =>
          s.observe(1.0) >> s.observe(2.0) >> s.observe(3.0) >>
            reg
              .summaryHistory(Summary.Name("test_total"), Metric.CommonLabels.empty)
              .assertEquals(Some(Chain(1.0, 2.0, 3.0)))
        }
    }
  }

  test("Summary - prefixed history") {
    TestingMetricRegistry[IO].flatMap { reg =>
      reg
        .createAndRegisterDoubleSummary(
          Some(Metric.Prefix("permutive")),
          Summary.Name("test_total"),
          Metric.Help("help"),
          Metric.CommonLabels.empty,
          Seq(Summary.QuantileDefinition(Summary.Quantile(0.5), Summary.AllowedError(0.1))),
          5.seconds,
          Summary.AgeBuckets(5)
        )
        .use { s =>
          s.observe(1.0) >> s.observe(2.0) >> s.observe(3.0) >>
            reg
              .summaryHistory(Some(Metric.Prefix("permutive")), Summary.Name("test_total"), Metric.CommonLabels.empty)
              .assertEquals(Some(Chain(1.0, 2.0, 3.0)))
        }
    }
  }

  test("Summary - labelled history") {
    TestingMetricRegistry[IO].flatMap { reg =>
      reg
        .createAndRegisterLabelledDoubleSummary(
          None,
          Summary.Name("test_total"),
          Metric.Help("help"),
          Metric.CommonLabels.empty,
          IndexedSeq(Label.Name("status")),
          Seq(Summary.QuantileDefinition(Summary.Quantile(0.5), Summary.AllowedError(0.1))),
          5.seconds,
          Summary.AgeBuckets(5)
        )((s: String) => IndexedSeq(s))
        .use { case s =>
          s.observe(1.0, "success") >> s.observe(2.0, "failure") >> reg
            .summaryHistory(
              Summary.Name("test_total"),
              Metric.CommonLabels.empty,
              IndexedSeq(Label.Name("status")),
              IndexedSeq("success")
            )
            .assertEquals(Some(Chain(1.0))) >> reg
            .summaryHistory(
              Summary.Name("test_total"),
              Metric.CommonLabels.empty,
              IndexedSeq(Label.Name("status")),
              IndexedSeq("failure")
            )
            .assertEquals(Some(Chain(2.0)))

        }
    }
  }

}
