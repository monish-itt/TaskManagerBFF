package taskbff

import cats.effect._
import cats.syntax.semigroupk._
import config.{ConfigLoader, TaskManagerConfig}
import doobie.hikari.HikariTransactor
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import taskbff.domian.repositories.{TagRepository, TaskRepository, UserRepository}
import taskbff.infrastructure.middleware.AuthMiddleware
import taskbff.infrastructure.routes.AuthRoutes.{ExpiredToken, InvalidToken, ValidToken}
import taskbff.infrastructure.routes.{AuthRoutes, TagRoutes, TaskRoutes, UserRoutes}

import scala.concurrent.ExecutionContext

object ServerApp extends IOApp {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {

    ConfigLoader.load[IO]().flatMap(config =>
      transactor(config).use { xa =>
        // Repositories
        val taskRepo = new TaskRepository(xa)
        val tagRepo = new TagRepository(xa)
        val userRepo = new UserRepository(xa)

        // Routes
        val taskRoutes = TaskRoutes.taskRoutes[IO](taskRepo)
        val tagRoutes = TagRoutes.tagRoutes[IO](tagRepo)
        val userRoutes = UserRoutes.userRoutes[IO](userRepo)
        val authRoutes = AuthRoutes.authRoutes[IO](userRepo)

        // Middleware for secured routes
        val tokenValidator: String => Either[String, String] = token =>
          AuthRoutes.validateJwtToken(token) match {
            case ValidToken(userId) => Right(userId)
            case ExpiredToken => Left("Token has expired")
            case InvalidToken(error) => Left(s"Invalid token: $error")
          }
        val securedTaskRoutes = AuthMiddleware[IO](tokenValidator)(Sync[IO], logger)(taskRoutes)
        val securedTagRoutes = AuthMiddleware[IO](tokenValidator)(Sync[IO], logger)(tagRoutes)

        // CORS Configuration
        val corsConfig = CORSConfig(
          anyOrigin = true,
          allowedMethods = Some(Set("GET", "POST", "PUT", "DELETE", "OPTIONS")),
          allowedHeaders = Some(Set("Content-Type", "Authorization")),
          allowCredentials = true,
          maxAge = 3600
        )

        // Combine Routes with CORS
        val httpApp = CORS(
          (
            authRoutes <+>
              userRoutes <+>
              securedTagRoutes <+>
              securedTaskRoutes
            ).orNotFound,
          corsConfig
        )

        // Start the server
        BlazeServerBuilder[IO](ExecutionContext.global)
          .bindHttp(config.serverPort, config.serverHost)
          .withHttpApp(httpApp)
          .resource
          .use { _ =>
            logger.info("Server started") >> IO.never
          }
          .as(ExitCode.Success)
      }
    )
  }

  private def transactor(config: TaskManagerConfig): Resource[IO, HikariTransactor[IO]] = {
    HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      config.dbURL,
      config.dbUsername,
      config.dbPassword,
      ExecutionContext.global
    )
  }
}