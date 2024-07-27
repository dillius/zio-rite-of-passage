package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.errors.HttpError
import sttp.tapir.*

trait BaseEndpoint {
  val baseEndpoint = 
    endpoint
      .errorOut(statusCode and plainBody[String])
      .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)

}
