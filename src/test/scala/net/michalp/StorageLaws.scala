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
import cats.effect.SyncIO

class IOStorageLaws extends Specification with Discipline with TestInstances {
  implicit val ticker: Ticker = Ticker()

  implicit def exhaustiveA[A](
    implicit arb: Arbitrary[A]
  ): ExhaustiveCheck[A] =
    ExhaustiveCheck.instance {
      List.fill(100)(Arbitrary.arbitrary[A].sample).flattenOption
    }

  def arbIOFunction[A: Cogen]: Arbitrary[A => IO[Unit]] = Arbitrary {
    Gen.function1[A, IO[Unit]](implicitly[Arbitrary[IO[Unit]]].arbitrary)
  }

  implicit def arbStorageIO: Arbitrary[Storage[IO, Int]] = Arbitrary {
    arbIOFunction[Int].arbitrary.map { generatedF =>
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

class SyncIOStorageLaws extends Specification with Discipline with TestInstances {
  implicit val ticker: Ticker = Ticker()

  implicit def exhaustiveA[A](
    implicit arb: Arbitrary[A]
  ): ExhaustiveCheck[A] =
    ExhaustiveCheck.instance {
      List.fill(100)(Arbitrary.arbitrary[A].sample).flattenOption
    }
  
  def arbSyncIOFunction[A: Cogen]: Arbitrary[A => SyncIO[Unit]] = Arbitrary {
    Gen.function1[A, SyncIO[Unit]](implicitly[Arbitrary[SyncIO[Unit]]].arbitrary)
  }

  implicit def arbStorageSyncIO: Arbitrary[Storage[SyncIO, Int]] = Arbitrary {
    arbSyncIOFunction[Int].arbitrary.map { generatedF =>
      new Storage[SyncIO, Int] {
        def store(a: Int): SyncIO[Unit] = generatedF(a)
      }
    }
  }


  checkAll(
    "Monoid[Storage[SyncIO, Int]]",
    MonoidTests[Storage[SyncIO, Int]](Storage.monoid[SyncIO, Int]).monoid
  )

}
