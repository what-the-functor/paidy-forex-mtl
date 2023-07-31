package forex.config

import cats.effect.Sync
import fs2.Stream

import cats.syntax.either._
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.ConfigReader
import org.http4s.Uri
import pureconfig.error.CannotConvert

object Config {

  /**
    * @param path the property path inside the default configuration
    */
  def stream[F[_]: Sync](path: String): Stream[F, ApplicationConfig] =
    Stream.eval(Sync[F].delay(ConfigSource.default.at(path).loadOrThrow[ApplicationConfig]))

  implicit val uriReader: ConfigReader[Uri] =
    ConfigReader[String].emap { s =>
      Uri.fromString(s).leftMap(e => CannotConvert(e.sanitized, "Uri", e.details))
    }
}
