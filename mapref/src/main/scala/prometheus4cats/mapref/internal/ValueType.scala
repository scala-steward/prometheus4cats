package prometheus4cats.mapref.internal

trait ValueType
object ValueType {
  case object DoubleValue extends ValueType
  case object LongValue extends ValueType
}
