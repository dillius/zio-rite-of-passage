package com.rockthejvm.reviewboard.repositories

import zio.*
import org.testcontainers.containers.PostgreSQLContainer
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

trait RepositorySpec {
  
  val initScript: String
  
  // test containers
  // spawn a Postgres instance on Docker just for the test
  private def createContainer() = {
    val container: PostgreSQLContainer[Nothing] = PostgreSQLContainer("postgres").withInitScript(initScript)
    container.start()
    container
  }

  // create a DataSource to connect
  private def createDataSource(container: PostgreSQLContainer[Nothing]): DataSource = {
    val dataSource = new PGSimpleDataSource()
    dataSource.setURL(container.getJdbcUrl)
    dataSource.setUser(container.getUsername)
    dataSource.setPassword(container.getPassword)

    dataSource
  }

  // use the DataSource for Quill
  val dataSourceLayer = ZLayer {
    for {
      container <- ZIO.acquireRelease(ZIO.attempt(createContainer()))(container => ZIO.attempt(container.close()).ignoreLogged)
      dataSource <- ZIO.attempt(createDataSource(container))
    } yield dataSource
  }
}
