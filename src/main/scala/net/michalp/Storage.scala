package net.michalp

import cats.syntax.all._
import cats.InvariantMonoidal
import cats.kernel.Monoid
import cats.Applicative

trait Storage[F[_], A] extends (A => F[Unit]) {
  def store(a: A): F[Unit]
  def apply(a: A): F[Unit] = store(a) // Make SAM work
}

object Storage {
  def apply[F[_], A](implicit ev: Storage[F, A]): Storage[F, A] = ev
  def unit[F[_]: InvariantMonoidal, A]: Storage[F, A] = _ => InvariantMonoidal[F].unit

  implicit def monoid[F[_]: Applicative, A] = new Monoid[Storage[F, A]] {
    override def combine(x: Storage[F, A], y: Storage[F, A]): Storage[F, A] =
      input => x.store(input) *> y.store(input)
    override def empty: Storage[F, A] = Storage.unit
  }

  implicit def eq[F[_], A](implicit equalFunction: cats.Eq[(A => F[Unit])]): cats.Eq[Storage[F, A]] = equalFunction.narrow
}
