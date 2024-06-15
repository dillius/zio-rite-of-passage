package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.services.CompanyService
import com.rockthejvm.reviewboard.syntax.*
import sttp.client3.testing.SttpBackendStub
import sttp.client3.*
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import zio.*
import zio.test.*
import zio.json.*

object CompanyControllerSpec extends ZIOSpecDefault {

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val rtjvm = Company(1, "rock-the-jvm", "Rock the JVM", "rockthejvm.com")
  private val serviceStub = new CompanyService {
    override def create(req: CreateCompanyRequest): Task[Company] =
      ZIO.succeed(rtjvm)

    override def getById(id: Long): Task[Option[Company]] =
      ZIO.succeed {
        if (id == 1) Some(rtjvm)
        else None
      }

    override def getBySlug(slug: String): Task[Option[Company]] =
      ZIO.succeed {
        if(slug == "rock-the-jvm") Some(rtjvm)
        else None
      }

    override def getAll: Task[List[Company]] =
      ZIO.succeed(List(rtjvm))
  }

  private def backendStubZIO(endpointFun: CompanyController => ServerEndpoint[Any, Task]) =
    for {
      controller <- CompanyController.makeZIO
      backendStub <- ZIO.succeed(TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(endpointFun(controller))
        .backend())
    } yield backendStub

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyControllerSpec")(
      test("simple test") {
        assertZIO(ZIO.succeed(1 + 1))(
          Assertion.assertion("basic math")(_ == 2)
        )
      },
      test("post company") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/companies")
            .body(CreateCompanyRequest("Rock the JVM", "rockthejvm.com").toJson)
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
            respBody.toOption.flatMap(_.fromJson[Company].toOption)
              .contains(Company(1, "rock-the-jvm", "Rock the JVM", "rockthejvm.com"))
          }
      },

      test("get all") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
            respBody.toOption
              .flatMap(_.fromJson[List[Company]].toOption)
              .contains(List(rtjvm))
          }
      },

      test("get by id") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
            respBody.toOption
              .flatMap(_.fromJson[Company].toOption)
              .contains(rtjvm)
          }
      }
    )
      .provide(ZLayer.succeed(serviceStub))
}
