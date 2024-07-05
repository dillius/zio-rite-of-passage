package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.syntax.*
import zio.*
import zio.test.*

import java.time.Instant

object ReviewRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/reviews.sql"

  val goodReview = Review(
    id = 1L,
    companyId = 1L,
    userId = 1L,
    management = 5,
    culture = 5,
    salaries = 5,
    benefits = 5,
    wouldRecommend = 10,
    review = "all good",
    created = Instant.now(),
    updated = Instant.now()
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ReviewRepositorySpec")(
      test("create review") {
        val program = for {
          repo <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
        } yield review

        program.assert { review =>
          review.management == goodReview.management &&
            review.culture == goodReview.culture &&
            review.salaries == goodReview.salaries &&
            review.benefits == goodReview.benefits &&
            review.wouldRecommend == goodReview.wouldRecommend &&
            review.review == goodReview.review
        }
      },
      test("get review by ids (id, companyId, userId)") {
        for {
          repo <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
          fetchedReview <- repo.getById(review.id)
          fetchedReview2 <- repo.getByCompanyId(review.companyId)
          fetchedReview3 <- repo.getByUserId(review.userId)
        } yield assertTrue (
          fetchedReview.contains(review) &&
            fetchedReview2.contains(review) &&
            fetchedReview3.contains(review)
        )
      }
    ).provide(
      ReviewRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
}
