package prometheus4cats.mapref.internal

import java.util.concurrent.ConcurrentHashMap

import cats.{Applicative, Order}
import cats.data.NonEmptySeq
import cats.effect.kernel.Concurrent
import io.chrisdavenport.mapref.MapRef
import prometheus4cats.{Counter, Gauge, Histogram, Label, Metric}
import cats.syntax.functor._
import cats.syntax.show._

import scala.math.Ordered.orderingToOrdered

trait MetricValues[F[_]] {
  def values: F[Iterable[MetricValues.Value]]
}

object MetricValues {
  type V = Either[Long, Double]

  case class Value(name: String, labels: IndexedSeq[(Label.Name, String)], value: V)

  case class CounterValues[F[_]: Concurrent](
    prefix: Option[Metric.Prefix],
    name: Counter.Name,
    map: scala.collection.concurrent.Map[IndexedSeq[(Label.Name, String)], V] =
      scala.collection.concurrent.TrieMap.empty
  ) extends MetricValues[F] {
    private val valueMap = MapRef.fromScalaConcurrentMap(map)

    def inc(labels: IndexedSeq[(Label.Name, String)], v: Double): F[Unit] = inc(labels, Right(v))
    def inc(labels: IndexedSeq[(Label.Name, String)], v: Long): F[Unit] = inc(labels, Left(v))

    def inc(labels: IndexedSeq[(Label.Name, String)], v: V): F[Unit] =
      valueMap(labels).update({
        case Some(Right(d)) => Some(v.fold[V](l => Right(d + l), d0 => Right(d0 + d)))
        case Some(Left(l)) => Some(v.fold[V](l0 => Left(l + l0), d => Right(l + d)))
        case None => Some(v)
      })

    override def values: F[Iterable[Value]] = Applicative[F].unit.as(map.map { case (labels, v) =>
      Value(name.show, labels, v)
    })
  }

  case class GaugeValues[F[_]: Concurrent](
    prefix: Option[Metric.Prefix],
    name: Gauge.Name,
    map: scala.collection.concurrent.Map[IndexedSeq[(Label.Name, String)], V] =
      scala.collection.concurrent.TrieMap.empty
  ) extends MetricValues[F] {
    private val valueMap = MapRef.fromScalaConcurrentMap(map)

    def inc(labels: IndexedSeq[(Label.Name, String)], v: V): F[Unit] =
      valueMap(labels).update({
        case Some(Right(d)) => Some(v.fold[V](l => Right(d + l), d0 => Right(d0 + d)))
        case Some(Left(l)) => Some(v.fold[V](l0 => Left(l + l0), d => Right(l + d)))
        case None => Some(v)
      })

    def dec(labels: IndexedSeq[(Label.Name, String)], v: V): F[Unit] =
      valueMap(labels).update({
        case Some(Right(d)) => Some(v.fold[V](l => Right(d - l), d0 => Right(d0 - d)))
        case Some(Left(l)) => Some(v.fold[V](l0 => Left(l - l0), d => Right(l - d)))
        case None => Some(v)
      })

    def set(labels: IndexedSeq[(Label.Name, String)], v: V): F[Unit] =
      valueMap(labels).set(Some(v))

    override def values: F[Iterable[Value]] = Applicative[F].unit.as(map.map { case (labels, v) =>
      Value(name.show, labels, v)
    })
  }

  case class HistogramValues[F[_]: Concurrent, N: Ordered: Order](
    prefix: Option[Metric.Prefix],
    name: Histogram.Name,
    buckets: NonEmptySeq[N],
    map: scala.collection.concurrent.Map[IndexedSeq[(Label.Name, String)], (N, Map[String, N])] =
      scala.collection.concurrent.TrieMap.empty
  )(implicit N: Numeric[N]) {
    private val valueMap = MapRef.fromScalaConcurrentMap(map)

    private val allbuckets: NonEmptySeq[N] =
      if (buckets.exists(_ == N.zero)) buckets.sorted else NonEmptySeq(N.zero, buckets.sorted.toSeq)

    // TODO maybe make this mutable
    private def updateBuckets(map: Map[String, N], v: N) =
      allbuckets
        .foldLeft(map)((values, bucket) =>
          if (v <= bucket)
            values.updatedWith(bucket.toString)(opt => Some(opt.fold(N.one)(N.plus(_, N.one))))
          else values
        )
        .updatedWith("+Inf") { opt =>
          val update = if (v > buckets.last) N.one else N.zero
          Some(opt.fold(update)(N.plus(_, update)))
        }

    def observe(labels: IndexedSeq[(Label.Name, String)], v: N): F[Unit] =
      valueMap(labels).update({
        case Some((sum, values)) => Some((N.plus(sum, v), updateBuckets(values, v)))
        case None =>
          Some(v -> updateBuckets(Map.empty, v))
      })
  }
}
