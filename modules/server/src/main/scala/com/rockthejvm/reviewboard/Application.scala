package com.rockthejvm.reviewboard

import com.rockthejvm.reviewboard.config.*
import com.rockthejvm.reviewboard.http.HttpApi
import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.services.*
import sttp.tapir.*
import sttp.tapir.server.ziohttp.*
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    server <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default // Can add configs e.g. CORS
      ).toHttp(endpoints)
    )
    _ <- Console.printLine("Rock the JVM!")
  } yield ()

  override def run =
    serverProgram.provide(
      Server.default,
      // configs
      Configs.makeLayer[JWTConfig]("rockthejvm.jwt"),
      // services
      CompanyServiceLive.layer,
      ReviewServiceLive.layer,
      UserServiceLive.layer,
      JWTServiceLive.layer,
      EmailServiceLive.layer,
      // repos
      CompanyRepositoryLive.layer,
      ReviewRepositoryLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokenRepositoryLive.layer,
      // other requirements
      Repository.dataLayer
    )
}
