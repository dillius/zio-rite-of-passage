package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.repositories.{RecoveryTokensRepository, UserRepository}
import zio.*
import zio.test.*

object UserServiceSpec extends ZIOSpecDefault {

  val daniel = User(
    1L,
    "daniel@rockthejvm.com",
    "1000:CEBF8CD09CEFB575E48CAFBAEF0B4E72705AC48D8BB37C7A:2BF6A1C435A7CD59BEC340D2E4A0D3CD308727B1B815FF1A"
  )

  val stubRepoLayer = ZLayer.succeed {
    new UserRepository {
      val db = collection.mutable.Map[Long, User](1L -> daniel)
      override def create(user: User): Task[User] = ZIO.succeed {
        db += (user.id -> user)
        user
      }
      override def update(id: Long, op: User => User): Task[User] = ZIO.attempt {
        val newUser = op(db(id))
        db += (newUser.id -> newUser)
        newUser
      }
      override def getById(id: Long): Task[Option[User]] = ZIO.succeed(db.get(id))
      override def getByEmail(email: String): Task[Option[User]] =
        ZIO.succeed(db.values.find(_.email == email))
      override def delete(id: Long): Task[User] = ZIO.attempt {
        val user = db(id)
        db -= id
        user
      }
    }
  }

  val stubTokenRepoLayer = ZLayer.succeed {
    new RecoveryTokensRepository {
      val db = collection.mutable.Map[String, String]()

      override def getToken(email: String): Task[Option[String]] =
        ZIO.attempt {
          val token = util.Random.alphanumeric.take(8).mkString.toUpperCase
          db += (email -> token)
          Some(token)
        }

      override def checkToken(email: String, token: String): Task[Boolean] =
        ZIO.succeed(db.get(email).filter(_ == token).nonEmpty)
    }
  }

  val stubEmailsLayer = ZLayer.succeed {
    new EmailService:
      override def sendEmail(to: String, subject: String, content: String): Task[Unit] = ZIO.unit
  }

  val stubJwtLayer = ZLayer.succeed {
    new JWTService:
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.email, "BIG ACCESS", Long.MaxValue))

      override def verifyToken(token: String): Task[UserID] =
        ZIO.succeed(UserID(daniel.id, daniel.email))
  }

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserServiceSpec")(
      test("create and validate a user") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.registerUser(daniel.email, "rockthejvm")
          valid   <- service.verifyPassword(daniel.email, "rockthejvm")
        } yield assertTrue(valid)
      },
      test("validate correct credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(daniel.email, "rockthejvm")
        } yield assertTrue(valid)
      },
      test("invalidate incorrect credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(daniel.email, "somethingelse")
        } yield assertTrue(!valid)
      },
      test("invalidate non-existent user") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword("someone@gmail.com", "somethingelse")
        } yield assertTrue(!valid)
      },
      test("update password") {
        for {
          service  <- ZIO.service[UserService]
          newUser  <- service.updatePassword(daniel.email, "rockthejvm", "scalarulez")
          oldValid <- service.verifyPassword(daniel.email, "rockthejvm")
          newValid <- service.verifyPassword(daniel.email, "scalarulez")
        } yield assertTrue(newValid && !oldValid)
      },
      test("delete non-existent user should fail") {
        for {
          service <- ZIO.service[UserService]
          err <- service.deleteUser("someone@gmail.com", "something").flip
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete with incorrect credentials should fail") {
        for {
          service <- ZIO.service[UserService]
          err <- service.deleteUser(daniel.email, "something").flip
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete user") {
        for {
          service <- ZIO.service[UserService]
          user <- service.deleteUser(daniel.email, "rockthejvm")
        } yield assertTrue(user.email == daniel.email)
      }
    ).provide(
      UserServiceLive.layer,
      stubJwtLayer,
      stubRepoLayer,
      stubEmailsLayer,
      stubTokenRepoLayer
    )
}
