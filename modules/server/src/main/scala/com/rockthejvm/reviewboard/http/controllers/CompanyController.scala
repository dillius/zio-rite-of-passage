package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.Company
import zio.*

import collection.mutable
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import com.rockthejvm.reviewboard.services.CompanyService
import sttp.tapir.server.ServerEndpoint

class CompanyController private (service: CompanyService) extends BaseController with CompanyEndpoints {
  // in-memory "database"
  

  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { req =>
    service.create(req)
  }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogicSuccess{ _ => service.getAll}

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogicSuccess { id =>
      ZIO
        .attempt(id.toLong)
        .flatMap(service.getById)
        .catchSome {
          case _ : java.lang.NumberFormatException =>
            service.getBySlug(id)
        }
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CompanyController {
  val makeZIO = for {
    service <- ZIO.service[CompanyService]
  } yield new CompanyController(service)
}
