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

package prometheus4cats

import cats.Show
import cats.syntax.all._

import prometheus4cats.internal.LabelNameFromStringLiteral
import prometheus4cats.internal.Refined
import prometheus4cats.internal.Refined.Regex

object Label {

  /** Refined value class for a label name that has been parsed from a string
    */
  final class Name private (val value: String) extends AnyVal with Refined.Value[String]

  object Name
      extends Regex[Name](
        "^(?!quantile$|le$)[a-zA-Z_:][a-zA-Z0-9_:]*$".r.pattern,
        new Name(_)
      )
      with LabelNameFromStringLiteral {
    // prevents macro compilation problems with the status label
    private[prometheus4cats] val outcomeStatus = new Name("outcome_status")

  }

  final class Value private (val value: String) extends AnyVal

  object Value extends ValueLowPriority0 {

    implicit def fromString(a: String): Value = new Value(a)

  }

  trait ValueLowPriority0 {
    implicit def fromShow[A: Show](a: A): Value = Value.fromString(a.show)
  }

}
