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

import scala.compiletime.ops.int.*
import scala.quoted.*

private[prometheus4cats] trait ShapelessPolyfill {

  type Represented[R] = R match {
    case IndexedSeq[a] => a
  }

  type Nat = Int

  object Nat {
    type _0 = 0
    type _1 = 1

    def toInt[N <: Nat](using toIntN: ToInt[N]) = toIntN.apply()
  }

  type Succ[N <: Nat] = N + 1

  trait ToInt[N <: Nat] {
    def apply(): Int
  }

  object ToInt {
    given default[N <: Nat](using vo: ValueOf[N]): ToInt[N] = new ToInt[N] {
      override def apply(): Int = vo.value
    }
  }

  trait GT[A <: Nat, B <: Nat] extends Serializable
  object GT {
    given gt1[B <: Nat]: GT[S[B], Nat._0] = new GT[S[B], Nat._0] {}
    given gt2[A <: Nat, B <: Nat](using GT[A, B]): GT[S[A], S[B]] = new GT[S[A], S[B]] {}
  }

  type TupleSized[R, A, N <: Int] <: Tuple = N match {
    case 0 => EmptyTuple
    case S[n] => A *: TupleSized[R, A, n]
  }

  extension [R, A, N <: Int](s: TupleSized[R, A, N]) {
    def unsized: IndexedSeq[A] =
      s.productIterator.toIndexedSeq.asInstanceOf[IndexedSeq[A]]
    def :+(a: A): TupleSized[R, A, N + 1] =
      (s :* a).asInstanceOf[TupleSized[R, A, N + 1]]
  }

  type Sized[Repr, L <: Nat] = TupleSized[Repr, Represented[Repr], L]

  object Sized {
    def apply[A](a1: A): Sized[IndexedSeq[A], 1] = Tuple1(a1)
  }

}
