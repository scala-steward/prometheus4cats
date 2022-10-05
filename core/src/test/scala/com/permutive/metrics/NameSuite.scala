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

package com.permutive.metrics

import com.permutive.metrics.Fixtures.{alphaChars, ordinaryChars}
import munit.ScalaCheckSuite
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Gen}

trait NameSuite[A] { self: ScalaCheckSuite =>
  val suffix = ""

  def make(str: String): Either[String, A]
  def stringValue(result: A): String

  implicit val stringArb: Arbitrary[String] = Arbitrary(Gen.alphaNumStr)

  test("names must not be empty") {
    assert(make("").isLeft)
  }

  property("names must start with an alpha char") {
    implicit val charArb: Arbitrary[Char] = Arbitrary(Gen.oneOf(alphaChars))

    forAll { (s: String, c: Char, i: Int) =>
      assert(make(s"$i$s$suffix").isLeft)

      val v = s"$c$s$suffix"
      assertEquals(make(v).map(stringValue), Right(v))
    }
  }

  property("names must not contain special chars") {
    implicit val stringArb: Arbitrary[String] = Arbitrary(Gen.asciiStr.suchThat(!_.forall(ordinaryChars.contains)))
    implicit val charArb: Arbitrary[Char] = Arbitrary(Gen.asciiChar.suchThat(!ordinaryChars.contains(_)))

    forAll { (s: String, c: Char) =>
      assert(make(s"$c$s$suffix").isLeft)
    }
  }
}
