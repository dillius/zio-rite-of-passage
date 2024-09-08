package com.rockthejvm.reviewboard

import com.rockthejvm.reviewboard.config.*
import com.rockthejvm.reviewboard.http.HttpApi
import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.services.*
import sttp.tapir.*
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.ziohttp.*
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default.appendInterceptor(
          CORSInterceptor.default
        )
      ).toHttp(endpoints)
    )
    _ <- Console.printLine("Rock the JVM!")
  } yield ()

  override def run =
    serverProgram.provide(
      Server.default,
      // configs
      // services
      CompanyServiceLive.layer,
      ReviewServiceLive.layer,
      UserServiceLive.layer,
      JWTServiceLive.configuredLayer,
      EmailServiceLive.configuredLayer,
      // repos
      CompanyRepositoryLive.layer,
      ReviewRepositoryLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokensRepositoryLive.configuredLayer,
      // other requirements
      Repository.dataLayer
    )
}
