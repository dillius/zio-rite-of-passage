package com.rockthejvm.reviewboard

import com.rockthejvm.reviewboard.http.HttpApi
import com.rockthejvm.reviewboard.repositories.{CompanyRepositoryLive, Repository}
import com.rockthejvm.reviewboard.services.{CompanyService, CompanyServiceLive}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
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
      // services
      CompanyServiceLive.layer,
      // repos
      CompanyRepositoryLive.layer,
      // other requirements
      Repository.dataLayer
    )
}
