package openmetrics4s.otel

import java.util

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.{InstrumentType, SdkMeterProvider}
import io.opentelemetry.sdk.metrics.`export`.{
  CollectionRegistration,
  MetricExporter,
  MetricReader,
  PeriodicMetricReader
}
import io.opentelemetry.sdk.metrics.data.{AggregationTemporality, MetricData}
import io.opentelemetry.sdk.metrics.internal.`export`.MetricProducer

import scala.jdk.CollectionConverters._

object Test extends App {
  val exporter = new MetricExporter {
    override def `export`(metrics: util.Collection[MetricData]): CompletableResultCode = {
      println(metrics)
      CompletableResultCode.ofSuccess()
    }

    override def flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override def shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override def getAggregationTemporality(instrumentType: InstrumentType): AggregationTemporality =
      AggregationTemporality.CUMULATIVE
  }

  val reader = PeriodicMetricReader.create(exporter)

  var prod: MetricProducer = MetricProducer.noop()

  val newReader = new MetricReader {

    override def register(registration: CollectionRegistration): Unit =
      prod = MetricProducer.asMetricProducer(registration)

    override def forceFlush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override def shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override def getAggregationTemporality(instrumentType: InstrumentType): AggregationTemporality =
      AggregationTemporality.CUMULATIVE
  }

  val x = SdkMeterProvider.builder.registerMetricReader(newReader).build()

  val meter = x.meterBuilder("test").build()

  val m1 = meter.upDownCounterBuilder("test").build()

  m1.add(1)

  val m2 = meter.upDownCounterBuilder("test").buildWithCallback(_.record(1))
  m2.close()

  meter.histogramBuilder("sdfsdf").build().record()

//  m2.add(1)

  reader.forceFlush()

  println(prod.collectAllMetrics().asScala.toList)
}
