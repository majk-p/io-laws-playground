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

class DummyConsumerLawsSpec extends Specification with Discipline {

  import cats.effect.unsafe.implicits.global

  def unsafeRun[A](ioA: IO[A]): A = ioA.unsafeRunSync()

  implicit def arbitraryIO[A: Arbitrary] = Arbitrary {
    implicitly[Arbitrary[A]].arbitrary.map(IO.pure(_))
  }

  implicit def eqIO[A: Eq]: Eq[IO[A]] = Eq.by[IO[A], A](unsafeRun)

  implicit def cogenIO[A: Cogen] =
    Cogen((seed: Seed, x: IO[A]) => Cogen[A].perturb(seed.next, unsafeRun(x)))

  implicit def exhaustiveFunctionByExamples[M[_], A: Cogen](
    implicit arb: Arbitrary[M[Unit]]
  ): ExhaustiveCheck[A => M[Unit]] =
    ExhaustiveCheck.instance {
      List.fill(100)(Arbitrary.arbitrary[A => M[Unit]].sample).flattenOption
    }

  implicit def arbF[A: Arbitrary]: Arbitrary[(A => IO[Unit]) => IO[Unit]] = Arbitrary {
    implicit val argCogen: Cogen[A => IO[Unit]] = Cogen.function1[A, IO[Unit]]
    Gen.function1[(A => IO[Unit]), IO[Unit]](implicitly[Arbitrary[IO[Unit]]].arbitrary)
  }

  implicit def arbConsumer: Arbitrary[Consumer[IO, Int]] = Arbitrary {
    arbF[Int].arbitrary.map { generatedF =>
      new Consumer[IO, Int] {
        override def consume(f: Int => IO[Unit]): IO[Unit] = generatedF(f)
      }
    }
  }

  checkAll(
    "Monoid[Consumer[IO, Int]]",
    MonoidTests[Consumer[IO, Int]](Consumer.monoid[IO, Int]).monoid
  )

}
