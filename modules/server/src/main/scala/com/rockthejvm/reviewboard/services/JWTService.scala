package com.rockthejvm.reviewboard.services

import com.auth0.jwt.JWTVerifier.BaseVerification
import com.auth0.jwt.{JWT, JWTVerifier}
import com.auth0.jwt.algorithms.Algorithm
import com.rockthejvm.reviewboard.domain.data.*
import zio.*

import java.time.Instant

trait JWTService {
  def createToken(user: User): Task[UserToken]
  def verifyToken(token: String): Task[UserID]
}

class JWTServiceLive(clock: java.time.Clock) extends JWTService {
  private val SECRET         = "secret"       // TODO pass this from config
  private val TTL            = 30 * 24 * 3600 // TODO pass this from config
  private val ISSUER         = "rockthejvm.com"
  private val CLAIM_USERNAME = "username"

  private val algorithm = Algorithm.HMAC512(SECRET)

  private val verifier: JWTVerifier =
    JWT
      .require(algorithm)
      .withIssuer(ISSUER)
      .asInstanceOf[BaseVerification]
      .build(clock)

  override def createToken(user: User): Task[UserToken] =
    for {
      now <- ZIO.attempt(clock.instant())
      expiration = now.plusSeconds(TTL)
      token <- ZIO.attempt(
        JWT
          .create()
          .withIssuer(ISSUER)
          .withIssuedAt(now)
          .withExpiresAt(expiration)
          .withSubject(user.id.toString) // user identifier
          .withClaim(CLAIM_USERNAME, user.email)
          .sign(algorithm)
      )
    } yield UserToken(user.email, token, expiration.getEpochSecond)

  override def verifyToken(token: String): Task[UserID] =
    for {
      decoded <- ZIO.attempt(verifier.verify(token))
      userId <- ZIO.attempt(
        UserID(
          decoded.getSubject().toLong,
          decoded.getClaim(CLAIM_USERNAME).asString()
        )
      )
    } yield userId
}

object JWTServiceLive {
  val layer = ZLayer {
    Clock.javaClock.map(new JWTServiceLive(_))
  }
}

object JWTServiceDemo extends ZIOAppDefault {
  val program = for {
    service <- ZIO.service[JWTService]
    token   <- service.createToken(User(1L, "daniel@rockthejvm.com", "unimporant"))
    _       <- Console.printLine(token)
    userId  <- service.verifyToken(token.token)
    _       <- Console.printLine(userId.toString)
  } yield ()
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.provide(JWTServiceLive.layer)
}
