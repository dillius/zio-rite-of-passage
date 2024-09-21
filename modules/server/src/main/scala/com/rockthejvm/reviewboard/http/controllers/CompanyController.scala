package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.{Company, UserID}
import zio.*

import collection.mutable
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import com.rockthejvm.reviewboard.services.{CompanyService, JWTService}
import sttp.tapir.server.ServerEndpoint

class CompanyController private (service: CompanyService, jwtService: JWTService)
    extends BaseController
    with CompanyEndpoints {
  // in-memory "database"

  val create: ServerEndpoint[Any, Task] = createEndpoint
    .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
    .serverLogic(_ => req => service.create(req).either)

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogic { _ => service.getAll.either }

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic { id =>
      ZIO
        .attempt(id.toLong)
        .flatMap(service.getById)
        .catchSome { case _: java.lang.NumberFormatException =>
          service.getBySlug(id)
        }
        .either
    }

  val allFilters: ServerEndpoint[Any, Task] =
    allFiltersEndpoint.serverLogic { _ =>
      service.allFilters.either
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, allFilters, getById)
}

object CompanyController {
  val makeZIO = for {
    companyService <- ZIO.service[CompanyService]
    jwtService     <- ZIO.service[JWTService]
  } yield new CompanyController(companyService, jwtService)
}
