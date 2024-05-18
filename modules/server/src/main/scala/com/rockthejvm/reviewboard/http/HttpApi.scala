package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.http.controllers.*

object HttpApi {

  def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  def makeControllers = for {
    health <- HealthController.makeZIO
    companies <- CompanyController.makeZIO
  } yield List(health, companies)

  val endpointsZIO = makeControllers.map(gatherRoutes)
}
