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

package prometheus4cats.internal

import prometheus4cats.Exemplar

import scala.reflect.macros.blackbox

private[prometheus4cats] trait ExemplarLabelNameFromStringLiteral {

  def apply(t: String): Exemplar.LabelName =
    macro ExemplarLabelNameMacros.fromStringLiteral

  implicit def fromStringLiteral(t: String): Exemplar.LabelName =
    macro ExemplarLabelNameMacros.fromStringLiteral

}

private[prometheus4cats] class ExemplarLabelNameMacros(val c: blackbox.Context) extends MacroUtils {

  def fromStringLiteral(t: c.Expr[String]): c.Expr[Exemplar.LabelName] = {
    val string: String = literal(t, or = "Exemplar.LabelName.from({string})")

    Exemplar.LabelName
      .from(string)
      .fold(
        abort,
        _ => c.universe.reify(Exemplar.LabelName.from(t.splice).toOption.get)
      )
  }

}
