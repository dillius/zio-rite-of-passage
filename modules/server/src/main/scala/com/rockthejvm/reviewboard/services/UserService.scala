package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.repositories.{RecoveryTokensRepository, UserRepository}
import zio.*

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

trait UserService {
  def registerUser(email: String, password: String): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
  def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User]
  def deleteUser(email: String, password: String): Task[User]
  def generateToken(email: String, password: String): Task[Option[UserToken]]
  def sendPasswordRecoveryToken(email: String): Task[Unit]
  def recoverPasswordFromToken(email: String, token: String, newPassword: String): Task[Boolean]
}

class UserServiceLive private (
    userRepo: UserRepository,
    jwtService: JWTService,
    emailService: EmailService,
    tokenRepo: RecoveryTokensRepository
) extends UserService {
  override def registerUser(email: String, password: String): Task[User] =
    userRepo.create(
      User(
        id = -1L,
        email = email,
        hashedPassword = UserServiceLive.Hasher.generateHash(password)
      )
    )

  override def verifyPassword(email: String, password: String): Task[Boolean] =
    for {
      existingUser <- userRepo.getByEmail(email)
      result <- existingUser match {
        case Some(user) =>
          ZIO.attempt(UserServiceLive.Hasher.validateHash(password, user.hashedPassword))
        case _ => ZIO.succeed(false)
      }
    } yield result

  override def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"cannot verify user $email existing"))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(oldPassword, existingUser.hashedPassword)
      )
      updatedUser <- userRepo
        .update(
          existingUser.id,
          user => user.copy(hashedPassword = UserServiceLive.Hasher.generateHash(newPassword))
        )
        .when(verified)
        .someOrFail(new RuntimeException(s"Could not update password for $email"))
    } yield updatedUser

  override def deleteUser(email: String, password: String): Task[User] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"cannot verify user $email existing"))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      )
      updatedUser <- userRepo
        .delete(existingUser.id)
        .when(verified)
        .someOrFail(new RuntimeException(s"Could not delete user for $email"))
    } yield updatedUser

  override def generateToken(email: String, password: String): Task[Option[UserToken]] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"cannot verify user $email existing"))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      )
      maybeToken <- jwtService.createToken(existingUser).when(verified)
    } yield maybeToken

  override def sendPasswordRecoveryToken(email: String): Task[Unit] =
    tokenRepo.getToken(email).flatMap {
      case Some(token) =>
        emailService.sendPasswordRecoveryEmail(email, token)
      case None => ZIO.unit
    }

  override def recoverPasswordFromToken(
      email: String,
      token: String,
      newPassword: String
  ): Task[Boolean] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException("Non-existent user"))
      tokenIsValid <- tokenRepo.checkToken(email, token)
      result <- userRepo
        .update(
          existingUser.id,
          user => user.copy(hashedPassword = UserServiceLive.Hasher.generateHash(newPassword))
        )
        .when(tokenIsValid)
        .map(_.nonEmpty)
    } yield result
}

object UserServiceLive {
  val layer = ZLayer {
    for {
      repo         <- ZIO.service[UserRepository]
      jwtService   <- ZIO.service[JWTService]
      emailService <- ZIO.service[EmailService]
      tokenRepo    <- ZIO.service[RecoveryTokensRepository]
    } yield new UserServiceLive(repo, jwtService, emailService, tokenRepo)
  }

  object Hasher {
    private val PBKDF2_ALGORITHM: String = "PBKDF2WithHmacSHA512"
    private val PBKDF2_ITERATIONS: Int   = 1000
    private val SALT_BYTE_SIZE: Int      = 24
    private val HASH_BYTE_SIZE: Int      = 24
    private val skf: SecretKeyFactory    = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)

    // string + salt + nIterations -- PBKDF2
    // "1000:AAAAAAAAAAAA:BBBBBBBBBBB"
    private def pbkdf2(
        message: Array[Char],
        salt: Array[Byte],
        iterations: Int,
        nBytes: Int
    ): Array[Byte] = {
      val keySpec: PBEKeySpec = new PBEKeySpec(message, salt, iterations, nBytes * 8)
      skf.generateSecret(keySpec).getEncoded
    }

    private def toHex(array: Array[Byte]): String =
      array.map(b => "%02X".format(b)).mkString

    private def fromHex(string: String): Array[Byte] = {
      string.sliding(2, 2).toArray.map { hexValue =>
        Integer.parseInt(hexValue, 16).toByte
      }
    }

    private def compareBytes(a: Array[Byte], b: Array[Byte]): Boolean = {
      val range = 0 until math.min(a.length, b.length)
      val diff = range.foldLeft(a.length ^ b.length) { case (acc, i) =>
        acc | (a(i) ^ b(i))
      }
      diff == 0
    }

    def generateHash(pass: String): String = {
      val rng: SecureRandom = new SecureRandom()
      val salt: Array[Byte] = Array.ofDim[Byte](SALT_BYTE_SIZE)
      rng.nextBytes(salt)
      val hashBytes = pbkdf2(pass.toCharArray, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
      s"$PBKDF2_ITERATIONS:${toHex(salt)}:${toHex(hashBytes)}"
    }

    def validateHash(string: String, hash: String): Boolean = {
      val hashSegments = hash.split(":")
      val nIterations  = hashSegments(0).toInt
      val salt         = fromHex(hashSegments(1))
      val validHash    = fromHex(hashSegments(2))
      val testHash     = pbkdf2(string.toCharArray, salt, nIterations, HASH_BYTE_SIZE)
      compareBytes(testHash, validHash)
    }
  }
}

object UserServiceDemo {
  def main(args: Array[String]) =
    println(UserServiceLive.Hasher.generateHash("rockthejvm"))
    println(
      UserServiceLive.Hasher.validateHash(
        "rockthejvm",
        "1000:CEBF8CD09CEFB575E48CAFBAEF0B4E72705AC48D8BB37C7A:2BF6A1C435A7CD59BEC340D2E4A0D3CD308727B1B815FF1A"
      )
    )
}
