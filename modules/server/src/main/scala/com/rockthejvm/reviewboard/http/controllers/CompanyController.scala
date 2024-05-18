package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.Company
import zio.*

import collection.mutable
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import sttp.tapir.server.ServerEndpoint

class CompanyController private extends BaseController with CompanyEndpoints {
  // in-memory "database"
  val db = mutable.Map[Long, Company](
    -1L -> Company(-1L, "invalid", "No Company", "nothing.com")
  )

  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { req =>
    ZIO.succeed {
      val newId = db.keys.max + 1
      val newCompany = req.toCompany(newId)
      db += (newId -> newCompany)
      newCompany
    }
  }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogicSuccess(_ => ZIO.succeed(db.values.toList))

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogicSuccess { id =>
      ZIO
        .attempt(id.toLong)
        .map(db.get)
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CompanyController {
  val makeZIO = ZIO.succeed(new CompanyController)
}
