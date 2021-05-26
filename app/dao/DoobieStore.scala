package dao

import javax.inject.Inject
import javax.sql.DataSource
import play.api.db.DBApi
import zio.Task
import zio.interop.catz._
import cats.effect.{Blocker, IO, Resource}
import doobie.util.ExecutionContexts
import doobie.Transactor


class DoobieStore @Inject()(dbApi: DBApi) {
  private val (txa, _) = transactor(dbApi.database("default").dataSource).allocated.unsafeRunSync()

  def getXa: Transactor[Task] = txa

  private def transactor(ds: DataSource): Resource[IO, Transactor[Task]] = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](32)
    be <- Blocker[IO]
  } yield Transactor.fromDataSource[Task](ds, ce, be)
}
