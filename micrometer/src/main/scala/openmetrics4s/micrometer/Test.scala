package openmetrics4s.micrometer

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}

object Test extends App {
  val reg = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

  reg.counter("test", "sdfds", "sdfsdf").increment(1.0)
  reg.counter("test", "sdfds", "sdfsdf", "dsfsdfs", "sdfsdf").increment(1.0)

  val x = DistributionStatisticConfig.builder

  val dist =
    DistributionSummary
      .builder("sdffs")
      .publishPercentileHistogram().publishPercentileHistogram()
      .maximumExpectedValue(1000d)
//      .minimumExpectedValue(0.001)
      .serviceLevelObjectives(1.0, 2.0, 1000)
      .register(reg)

  dist.record(1)
  dist.record(1000)

  println(reg.scrape())
}
