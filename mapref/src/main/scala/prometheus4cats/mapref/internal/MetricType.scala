package prometheus4cats.mapref.internal

trait MetricType
object MetricType {
  case object Metric extends MetricType
  case object Callback extends MetricType
}
