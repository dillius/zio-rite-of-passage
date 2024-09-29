package com.rockthejvm.reviewboard.repositories

import zio.*
import zio.test.*
import com.rockthejvm.reviewboard.syntax.*
import com.rockthejvm.reviewboard.domain.data.{Company, CompanyFilter}

import java.sql.SQLException
import javax.sql.DataSource

object CompanyRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/companies.sql"

  private val rtjvm = Company(1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com")

  private def genString() = scala.util.Random.alphanumeric.take(8).mkString

  private def genCompany(): Company =
    Company(
      -1L,
      slug = genString(),
      name = genString(),
      url = genString(),
      location = Some(genString()),
      country = Some(genString()),
      industry = Some(genString()),
      tags = (1 to 3).map(_ => genString()).toList
    )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyRepositorySpec")(
      test("create a company") {
        val program = for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(rtjvm)
        } yield company

        program.assert {
          case Company(_, "rock-the-jvm", "Rock the JVM", "rockthejvm.com", _, _, _, _, _) => true
          case _                                                                           => false
        }
      },
      test("create a duplicate company should error") {
        val program = for {
          repo <- ZIO.service[CompanyRepository]
          _    <- repo.create(rtjvm)
          err  <- repo.create(rtjvm).flip
        } yield err

        program.assert(_.isInstanceOf[SQLException])
      },
      test("get by id and slug") {
        val program = for {
          repo          <- ZIO.service[CompanyRepository]
          company       <- repo.create(rtjvm)
          fetchedById   <- repo.getById(company.id)
          fetchedBySlug <- repo.getBySlug(company.slug)
        } yield (company, fetchedById, fetchedBySlug)

        program.assert { case (company, fid, fslug) =>
          fid.contains(company) && fslug.contains(company)
        }
      },
      test("update record") {
        val program = for {
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(rtjvm)
          updated     <- repo.update(company.id, _.copy(url = "blog.rockthejvm.com"))
          fetchedByid <- repo.getById(company.id)
        } yield (updated, fetchedByid)

        program.assert { case (updated, fid) =>
          fid.contains(updated)
        }
      },
      test("delete record") {
        val program = for {
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(rtjvm)
          _           <- repo.delete(company.id)
          fetchedById <- repo.getById(company.id)
        } yield fetchedById

        program.assert(_.isEmpty)
      },
      test("get all records") {
        val program = for {
          repo             <- ZIO.service[CompanyRepository]
          companies        <- ZIO.collectAll((1 to 10).map(_ => repo.create(genCompany())))
          companiesFetched <- repo.get
        } yield (companies, companiesFetched)

        program.assert { case (companies, companiesFetched) =>
          companies.toSet == companiesFetched.toSet
        }
      },
      test("search by tag") {
        for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(genCompany())
          fetched <- repo.search(CompanyFilter(tags = company.tags.headOption.toList))
        } yield assertTrue {
          fetched.nonEmpty && fetched.tail.isEmpty && fetched.head == company
        }
      }
    ).provide(
      CompanyRepositoryLive.layer,
//      ZPostgreSQLContainer.Settings.default >>> ZPostgreSQLContainer.live,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
}
