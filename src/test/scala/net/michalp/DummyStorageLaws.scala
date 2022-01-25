package net.michalp

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import cats.effect.testkit.TestInstances
import cats.implicits._
import cats.kernel.Eq
import cats.kernel.laws.discipline.MonoidTests
import cats.laws.discipline.ExhaustiveCheck
import cats.laws.discipline.FunctorFilterTests
import cats.laws.discipline.MiniInt
import cats.laws.discipline.MonadTests
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.eq._
import org.scalacheck.Arbitrary
import org.scalacheck.Cogen
import org.scalacheck.Gen
import org.scalacheck.rng.Seed
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

class DummyStorageLaws extends Specification with Discipline {

  import cats.effect.unsafe.implicits.global

  def unsafeRun[A](ioA: IO[A]): A = ioA.unsafeRunSync()

  implicit def arbitraryIO[A: Arbitrary] = Arbitrary {
    implicitly[Arbitrary[A]].arbitrary.map(IO.pure(_))
  }

  implicit def eqIO[A: Eq]: Eq[IO[A]] = Eq.by[IO[A], A](unsafeRun)

  implicit def exhaustiveA[A](
    implicit arb: Arbitrary[A]
  ): ExhaustiveCheck[A] =
    ExhaustiveCheck.instance {
      List.fill(100)(Arbitrary.arbitrary[A].sample).flattenOption
    }

  implicit def arbF[A: Cogen]: Arbitrary[A => IO[Unit]] = Arbitrary {
    Gen.function1[A, IO[Unit]](implicitly[Arbitrary[IO[Unit]]].arbitrary)
  }

  implicit def arbStorage: Arbitrary[Storage[IO, Int]] = Arbitrary {
    arbF[Int].arbitrary.map { generatedF =>
      new Storage[IO, Int] {
        def store(a: Int): IO[Unit] = generatedF(a)
      }
    }
  }

  checkAll(
    "Monoid[Storage[IO, Int]]",
    MonoidTests[Storage[IO, Int]](Storage.monoid[IO, Int]).monoid
  )

}
