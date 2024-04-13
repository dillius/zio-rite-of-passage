package com.rockthejvm

import zio.*

import java.io.IOException
import scala.io.StdIn

object ZIORecap extends ZIOAppDefault {

  private val meaningOfLife: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
  val aFailure: ZIO[Any, String, Nothing] = ZIO.fail("Something went wrong")
  val aSuspension: ZIO[Any, Throwable, Int] = ZIO.suspend(meaningOfLife)

  val improvedMOL: ZIO[Any, Nothing, RuntimeFlags] = meaningOfLife.map(_ * 2)
  val printingMOL: ZIO[Any, Nothing, Unit] = meaningOfLife.flatMap(mol => ZIO.succeed(println(mol)))

  val smallProgram: ZIO[Any, IOException, Unit] = for {
    _ <- Console.printLine("what's your name?")
    name <- ZIO.succeed(StdIn.readLine())
    _ <- Console.printLine(s"Welcome to ZIO, $name")
  } yield ()

  private val anAttempt: ZIO[Any, Throwable, Int] = ZIO.attempt {
    println("Trying something")
    val string: String = null
    string.length
  }

  val catchError: ZIO[Any, Nothing, Any] = anAttempt.catchAll(e => ZIO.succeed(s"Returning some different value"))
  val catchSelecting: ZIO[Any, Throwable, Any] = anAttempt.catchSome {
    case e: RuntimeException => ZIO.succeed(s"Ignoring runtime exception: $e")
    case _ => ZIO.succeed("Ignoring everything else")
  }

  private val delayedValue = Random.nextIntBetween(0, 100).delay(1.second)
  val aPair: ZIO[Any, Nothing, (Int, Int)] = for {
    a <- delayedValue
    b <- delayedValue
  } yield (a,b) // 2 seconds

  val aPairPar: ZIO[Any, Nothing, (Int, Int)] = for {
    fibA <- delayedValue.fork
    fibB <- delayedValue.fork
    a <- fibA.join
    b <- fibB.join
  } yield (a,b) // 1 second

  val interruptedFiber: ZIO[Any, Nothing, Unit] = for {
    fib <- delayedValue.map(println).onInterrupt(ZIO.succeed(println("I'm interrupted!"))).fork
    _ <- ZIO.sleep(500.millis).as(println("Cancelling fiber")) *> fib.interrupt
    _ <- fib.join
  } yield ()

  val ignoredInterruption: ZIO[Any, Nothing, Unit] = for {
    fib <- ZIO.uninterruptible(delayedValue.map(println).onInterrupt(ZIO.succeed(println("I'm interrupted!")))).fork
    _ <- ZIO.sleep(500.millis).as(println("Cancelling fiber")) *> fib.interrupt
    _ <- fib.join
  } yield ()

  val aPairPar_v2: ZIO[Any, Nothing, (Int, Int)] = delayedValue.zipPar(delayedValue)
  val randomx10: ZIO[Any, Nothing, IndexedSeq[Int]] = ZIO.foreachPar(1 to 10)(_ => delayedValue)
  // collectAllPar, reduceAllPar, mergeAllPar, foreachPar

  case class User(name: String, email: String)
  class UserSubscription(emailService: EmailService, userDatabase: UserDatabase) {
    def subscribeUser(user: User): Task[Unit] = for {
      _ <- emailService.email(user)
      _ <- userDatabase.insert(user)
      _ <- ZIO.succeed(s"Subscribed $user")
    } yield ()
  }
  object UserSubscription {
    val live: ZLayer[EmailService with UserDatabase, Nothing, UserSubscription] =
      ZLayer.fromFunction((emailS, userD) => new UserSubscription(emailS, userD))
  }
  class EmailService {
    def email(user: User): Task[Unit] = ZIO.succeed(s"Emailed $user")
  }
  object EmailService {
    val live: ZLayer[Any, Nothing, EmailService] = ZLayer.succeed(new EmailService)
  }
  class UserDatabase(connectionPool: ConnectionPool) {
    def insert(user: User): Task[Unit] = ZIO.succeed(s"Inserted $user")
  }
  object UserDatabase {
    val live: ZLayer[ConnectionPool, Nothing, UserDatabase] =
      ZLayer.fromFunction(new UserDatabase(_))
  }
  class ConnectionPool(nConnections: Int) {
    def get: Task[Connection] = ZIO.succeed(Connection())
  }
  object ConnectionPool {
    def live(nConnections: Int): ZLayer[Any, Nothing, ConnectionPool] =
      ZLayer.succeed(ConnectionPool(nConnections))
  }
  case class Connection()

  private def subscribe(user: User): ZIO[UserSubscription, Throwable, Unit] = for {
    sub <- ZIO.service[UserSubscription]
    _ <- sub.subscribeUser(user)
  } yield ()

  private val program = for {
    _ <- subscribe(User("Daniel", "daniel@rockthejvm.com"))
    _ <- subscribe(User("Bon Jovi", "jon@rockthejvm.com"))
  } yield ()


  override def run: ZIO[Any, Throwable, Unit] = program.provide(
    ConnectionPool.live(10),
    UserDatabase.live,
    EmailService.live,
    UserSubscription.live
  )

}
