package forex.services.rates

import cats.Applicative
import interpreters._
import org.http4s.client.Client

import forex.config.OneFrameConfig
import cats.effect.Sync

object Interpreters {

  /** Dummy stand-in for testing purposes */
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  /** Makes HTTP requests with the provided [[org.http4s.client.Client]] */
  def live[F[_]: Sync](config: OneFrameConfig): Client[F] => Algebra[F] = client =>
    new OneFrameHttp[F](config)(client)
}
