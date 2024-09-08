package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*

trait ReviewEndpoints extends BaseEndpoint {
  val createEndpoint =
    secureBaseEndpoint
      .tag("Reviews")
      .name("create")
      .description("Add a review for a company")
      .in("reviews")
      .post
      .in(jsonBody[CreateReviewRequest])
      .out(jsonBody[Review])

  val getByIdEndpoint =
    baseEndpoint
      .tag("Reviews")
      .name("getById")
      .description("Get a review by its id")
      .in("reviews" / path[Long]("id"))
      .get
      .out(jsonBody[Option[Review]])

  val getByCompanyIdEndpoint =
    baseEndpoint
      .tag("Reviews")
      .name("getByCompanyId")
      .description("Get reviews for a company")
      .in("reviews" / "company" / path[Long]("id"))
      .get
      .out(jsonBody[List[Review]])

}
