package com.rockthejvm.reviewboard.http.controllers

import zio.*
import com.rockthejvm.reviewboard.http.endpoints.HealthEndpoint

class HealthController private extends HealthEndpoint {
  val health = healthEndpoint
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

}

object HealthController {
  val makeZIO = ZIO.succeed(new HealthController)
}
