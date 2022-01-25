package net.michalp

import cats.syntax.all._
import cats.InvariantMonoidal
import cats.kernel.Monoid
import cats.Applicative

trait Consumer[F[_], +A] extends ((A => F[Unit]) => F[Unit]) {
  def consume(f: A => F[Unit]): F[Unit]
  def apply(f: A => F[Unit]): F[Unit] = consume(f) // Make SAM work
}

object Consumer {
  def apply[F[_], A](implicit ev: Consumer[F, A]): Consumer[F, A] = ev
  def unit[F[_]: InvariantMonoidal]: Consumer[F, Nothing] = _ => InvariantMonoidal[F].unit

  implicit def monoid[F[_]: Applicative, A] = new Monoid[Consumer[F, A]] {
    override def combine(x: Consumer[F, A], y: Consumer[F, A]): Consumer[F, A] =
      input => x.consume(input) *> y.consume(input)
    override def empty: Consumer[F, A] = Consumer.unit
  }

  implicit def eq[F[_], A](implicit equalFunction: cats.Eq[(A => F[Unit]) => F[Unit]]): cats.Eq[Consumer[F, A]] = equalFunction.narrow
}
