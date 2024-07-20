package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.http.endpoints.ReviewEndpoints
import com.rockthejvm.reviewboard.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import zio.*

class ReviewController private(reviewService: ReviewService) extends BaseController with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint.serverLogicSuccess(req => reviewService.create(req, -1L /* TODO add user id */))
    
  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogicSuccess(id => reviewService.getById(id))
    
  val getByCompanyId: ServerEndpoint[Any, Task] =
    getByCompanyIdEndpoint.serverLogicSuccess(companyId => reviewService.getByCompanyId(companyId))
    
  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getById, getByCompanyId)
}

object ReviewController {
  val makeZIO = ZIO.service[ReviewService].map(service => new ReviewController(service))
}