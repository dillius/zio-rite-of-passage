package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.UserID
import com.rockthejvm.reviewboard.http.endpoints.ReviewEndpoints
import com.rockthejvm.reviewboard.services.{JWTService, ReviewService}
import sttp.tapir.server.ServerEndpoint
import zio.*

class ReviewController private (reviewService: ReviewService, jwtService: JWTService)
    extends BaseController
    with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
      .serverLogic(userId => req => reviewService.create(req, userId.id).either)

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic(id => reviewService.getById(id).either)

  val getByCompanyId: ServerEndpoint[Any, Task] =
    getByCompanyIdEndpoint.serverLogic(companyId => reviewService.getByCompanyId(companyId).either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getById, getByCompanyId)
}

object ReviewController {
  val makeZIO =
    for {
      reviewService <- ZIO.service[ReviewService]
      jwtService    <- ZIO.service[JWTService]
    } yield new ReviewController(reviewService, jwtService)
}
