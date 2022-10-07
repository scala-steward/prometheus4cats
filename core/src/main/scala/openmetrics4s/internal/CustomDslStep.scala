package openmetrics4s.internal

import openmetrics4s.{Metric, MetricsRegistry}

trait CustomDslStep[A, NextStep] {
  protected def apply(a: A): NextStep
}

object CustomDslStep {
  def create[B, NextStep](
      f: B => NextStep
  ) = new CustomDslStep[B, NextStep] {
    override protected def apply(a: B): NextStep = f(a)
  }
}
