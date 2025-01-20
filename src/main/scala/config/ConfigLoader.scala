package config

import cats.effect.Sync
import com.comcast.ip4s.Port
import pureconfig.error.CannotConvert
import pureconfig.generic.ProductHint
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigObjectSource, ConfigReader, ConfigSource}
import pureconfig.generic.auto._


case class TaskManagerConfig(
        serverPort: Int,
        serverHost: String,
        dbURL: String,
        dbUsername: String,
        dbPassword: String
)



object ConfigLoader {
  implicit val portConfigReader: ConfigReader[Port] = ConfigReader[String].emap { str =>
    Port.fromString(str).toRight(CannotConvert(str, "Port", "Invalid port format"))
  }
  def load[F[_]: Sync](configSource: ConfigObjectSource = ConfigSource.default): F[TaskManagerConfig] = {
    implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))
    configSource.loadF[F, TaskManagerConfig]()
  }

  preserveImportsFor[ConfigReader[Port]]
}
