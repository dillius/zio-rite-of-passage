package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.Review
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*

trait ReviewRepository {
  def create(review: Review): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(companyId: Long): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def update(id: Long, op: Review => Review): Task[Review]
  def delete(id: Long): Task[Review]
}

class ReviewRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {

  override def create(review: Review): Task[Review] = ZIO.fail(new RuntimeException("not implemented"))
  override def getById(id: Long): Task[Option[Review]] = ZIO.fail(new RuntimeException("not implemented"))
  override def getByCompanyId(companyId: Long): Task[List[Review]] = ZIO.fail(new RuntimeException("not implemented"))
  override def getByUserId(userId: Long): Task[List[Review]] = ZIO.fail(new RuntimeException("not implemented"))
  override def update(id: Long, op: Review => Review): Task[Review] = ZIO.fail(new RuntimeException("not implemented"))
  override def delete(id: Long): Task[Review] = ZIO.fail(new RuntimeException("not implemented"))
}

object ReviewRepositoryLive {
  val layer = ZLayer {
    for {
      quill <- ZIO.service[Quill.Postgres[SnakeCase.type]]
    } yield ReviewRepositoryLive(quill)
  }
}
