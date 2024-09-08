package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec
import java.time.Instant

final case class Review (
    id: Long, // PK
    companyId: Long, //FK
    userId: Long, //FK
    // scores
    management: Int,
    culture: Int,
    salaries: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String,
    created: Instant,
    updated: Instant
                        )

object Review {
  given code: JsonCodec[Review] = DeriveJsonCodec.gen[Review]
}
