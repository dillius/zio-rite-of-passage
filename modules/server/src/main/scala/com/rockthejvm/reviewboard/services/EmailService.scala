package com.rockthejvm.reviewboard.services

import zio.*

trait EmailService {
  def sendEmail(to: String, subject: String, content: String): Task[Unit]
  def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit]
}

class EmailServiceLive extends EmailService {
  override def sendEmail(to: String, subject: String, content: String): Task[Unit] =
    ZIO.fail(new RuntimeException("not implemented"))
  override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] =
    ZIO.fail(new RuntimeException("not implemented"))
}

object EmailServiceLive {
  val layer = ZLayer.succeed(new EmailServiceLive)
}
