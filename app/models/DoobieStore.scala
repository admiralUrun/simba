package models

import cats.effect._
import doobie._
import javax.inject.Inject
import javax.sql.DataSource
import play.api.db.DBApi


class DoobieStore @Inject()(dbApi: DBApi) {
  private implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)
  protected val (xa, release) = transactor(dbApi.database("default").dataSource).allocated.unsafeRunSync()
  def getXa(): DataSourceTransactor[IO] = xa
  private def transactor(ds: DataSource): Resource[IO, DataSourceTransactor[IO]] = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
    be <- Blocker[IO]   // our blocking EC
  } yield Transactor.fromDataSource[IO](ds, ce, be)
}
