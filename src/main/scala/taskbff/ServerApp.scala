package taskbff

import cats.effect.{ExitCode, IO, IOApp, Resource}
import doobie.hikari.HikariTransactor
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger

import scala.concurrent.ExecutionContext

object ServerApp extends IOApp {
  private def transactor: Resource[IO, HikariTransactor[IO]] = {
    HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql://localhost:5432/taskdb",
      "postgresuser",
      "postgrespassword",
      ExecutionContext.global
    )
  }

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    transactor.use { xa =>
      val taskRepo = new TaskRepository(xa)
      val httpApp = TaskRoutes.taskRoutes[IO](taskRepo).orNotFound

      BlazeServerBuilder[IO](ExecutionContext.global)
        .bindHttp(9080, "localhost")
        .withHttpApp(httpApp)
        .resource
        .use { _ =>
          logger.info("Server started on http://localhost:9080") >> IO.never
        }
        .as(ExitCode.Success)
    }
  }
}