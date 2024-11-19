package taskbff

import cats.effect.{Async, LiftIO}
import cats.implicits._
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{Challenge, EntityDecoder, EntityEncoder, HttpRoutes}
import org.typelevel.log4cats.Logger

import java.util.Date

object AuthRoutes {

  private val SecretKey = "your-very-secret-key"
  private val jwtAlgorithm = Algorithm.HMAC256(SecretKey)
  private val TokenExpirationTimeMs = 60 * 60 * 1000 // 1 hour

  implicit def loginEntityDecoder[F[_] : Async]: EntityDecoder[F, LoginRequest] = jsonOf[F, LoginRequest]
  implicit def loginEntityEncoder[F[_] : Async]: EntityEncoder[F, LoginResponse] = jsonEncoderOf[F, LoginResponse]

  def authRoutes[F[_] : Async : LiftIO : Logger](userRepo: UserRepository): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {

      // Login: Validate credentials and generate JWT
      case req @ POST -> Root / "login" =>
        for {
          loginRequest <- req.as[LoginRequest]
          userOpt <- LiftIO[F].liftIO(userRepo.getUserByUsernameAndPassword(loginRequest.username, loginRequest.password))
          response <- userOpt match {
            case Some(user) =>
              val token = generateJwtToken(user)
              Ok(LoginResponse(token))
            case None =>
              val challenge = Challenge("Bearer", "Invalid username or password")
              Unauthorized(challenge)
          }
        } yield response

      // Logout: (Optional) Clear token on the client
      case POST -> Root / "logout" =>
        Ok("Logged out successfully")
    }
  }

  private def generateJwtToken(user: User): String = {
    JWT.create()
      .withClaim("id", user.user_id)
      .withClaim("username", user.username)
      .withClaim("role", user.role_id)
      .withExpiresAt(new Date(System.currentTimeMillis() + TokenExpirationTimeMs))
      .sign(jwtAlgorithm)
  }

  def validateJwtToken(token: String): Either[String, String] = {
    try {
      val verifier = JWT.require(jwtAlgorithm).build()
      val decodedToken = verifier.verify(token)
      Right(decodedToken.getClaim("id").asString()) // Return user ID
    } catch {
      case e: JWTVerificationException => Left(e.getMessage)
    }
  }
}

case class LoginRequest(username: String, password: String)
case class LoginResponse(token: String)