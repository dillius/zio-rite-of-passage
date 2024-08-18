package com.rockthejvm.reviewboard.repositories

import zio.*

trait RecoveryTokensRepository {
  def getToken(email: String): Task[Option[String]]
  def checkToken(email: String, token: String): Task[Boolean]
}

class RecoveryTokenRepositoryLive extends RecoveryTokensRepository {
  override def getToken(email: String): Task[Option[String]] =
    ZIO.fail(new RuntimeException("not implemented"))
  override def checkToken(email: String, token: String): Task[Boolean] =
    ZIO.fail(new RuntimeException("not implemented"))
}

object RecoveryTokenRepositoryLive {
  def layer = ZLayer.succeed(new RecoveryTokenRepositoryLive)
}
