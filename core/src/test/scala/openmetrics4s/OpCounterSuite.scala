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

package openmetrics4s

import cats.effect.{IO, Ref}
import cats.syntax.semigroup._
import munit.{CatsEffectSuite, ScalaCheckSuite}
import openmetrics4s.OpCounter.Status
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop._

import scala.concurrent.duration._

class OpCounterSuite extends CatsEffectSuite with ScalaCheckSuite {
  val opCounter: IO[(OpCounter[IO], IO[Map[Status, Int]])] =
    Ref.of[IO, Map[Status, Int]](Map.empty).map { ref =>
      OpCounter.fromCounter(
        Counter.Labelled.make[IO, Int, Status](1, (i, s) => ref.update(_ |+| Map(s -> i)))
      ) -> ref.get
    }

  val labelledOpCounter: IO[(OpCounter.Labelled[IO, String], IO[Map[(String, Status), Int]])] =
    Ref.of[IO, Map[(String, Status), Int]](Map.empty).map { ref =>
      OpCounter.Labelled.fromCounter(
        Counter.Labelled.make[IO, Int, (String, Status)](1, (i, s) => ref.update(_ |+| Map(s -> i)))
      ) -> ref.get
    }

  implicit val posInt: Arbitrary[Int] = Arbitrary(Gen.posNum[Int])

  property("op counter should record success") {
    forAll { i: Int =>
      val nonZero = Math.abs(i) + 1

      opCounter.flatMap { case (counter, res) =>
        counter.surround(IO.unit).replicateA(nonZero) >> res.map(
          assertEquals(_, Map[Status, Int](Status.Succeeded -> nonZero))
        )
      }.unsafeRunSync()
    }
  }

  property("op counter should record cancelation") {
    forAll { i: Int =>
      val nonZero = Math.abs(i) + 1

      opCounter.flatMap { case (counter, res) =>
        // the deferred in the race here gives time for the finalizers to be registered on the first IO
        IO.deferred[Unit]
          .flatMap(wait => counter.surround(wait.complete(()) >> IO.sleep(10.minutes)).race(wait.get))
          .replicateA(nonZero) >> res
          .map(
            assertEquals(_, Map[Status, Int](Status.Canceled -> nonZero))
          )
      }.unsafeRunSync()
    }
  }

  property("op counter should record failure") {
    forAll { i: Int =>
      val nonZero = Math.abs(i) + 1

      opCounter.flatMap { case (counter, res) =>
        counter.surround(IO.raiseError(new RuntimeException())).attempt.replicateA(nonZero) >> res.map(
          assertEquals(_, Map[Status, Int](Status.Errored -> nonZero))
        )
      }.unsafeRunSync()
    }
  }

  property("op counter should record success with labels") {
    forAll { (i: Int, s: String) =>
      val nonZero = Math.abs(i) + 1

      labelledOpCounter.flatMap { case (counter, res) =>
        counter.surround(IO.unit, s).replicateA(nonZero) >> res.map(
          assertEquals(_, Map[(String, Status), Int]((s, Status.Succeeded) -> nonZero))
        )
      }.unsafeRunSync()
    }
  }

  property("op counter should record cancelation with labels") {
    forAll { (i: Int, s: String) =>
      val nonZero = Math.abs(i) + 1

      labelledOpCounter.flatMap { case (counter, res) =>
        // the deferred in the race here gives time for the finalizers to be registered on the first IO
        IO.deferred[Unit]
          .flatMap(wait => counter.surround(wait.complete(()) >> IO.sleep(10.minutes), s).race(wait.get))
          .replicateA(nonZero) >> res.map(
          assertEquals(_, Map[(String, Status), Int]((s, Status.Canceled) -> nonZero))
        )
      }.unsafeRunSync()
    }
  }

  property("op counter should record failure with labels") {
    forAll { (i: Int, s: String) =>
      val nonZero = Math.abs(i) + 1

      labelledOpCounter.flatMap { case (counter, res) =>
        counter.surround(IO.raiseError(new RuntimeException()), s).attempt.replicateA(nonZero) >> res.map(
          assertEquals(_, Map[(String, Status), Int]((s, Status.Errored) -> nonZero))
        )
      }.unsafeRunSync()
    }
  }

}
