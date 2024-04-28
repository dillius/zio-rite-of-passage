package com.rockthejvm.reviewboard

import com.rockthejvm.reviewboard.http.controllers.HealthController
import sttp.tapir.*
import sttp.tapir.server.ziohttp.*
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  val serverProgram = for {
    controller <- HealthController.makeZIO
    server <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default // Can add configs e.g. CORS
      ).toHttp(List(controller.health))
    )
    _ <- Console.printLine("Rock the JVM!")
  } yield ()

  override def run =
    serverProgram.provide(
      Server.default
    )
}
