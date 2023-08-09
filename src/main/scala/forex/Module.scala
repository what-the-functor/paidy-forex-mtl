package forex

import cats.effect.Concurrent
import cats.effect.Timer
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.AutoSlash
import org.http4s.server.middleware.Caching
import org.http4s.server.middleware.Timeout

import scala.concurrent.duration._

class Module[F[_]: Concurrent: Timer](client: Client[F], config: ApplicationConfig) {

  private val httpRatesService: Client[F] => RatesService[F] =
    RatesServices.live[F](config.oneFrame)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](httpRatesService(client))

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val cacheControMiddleware: TotalMiddleware =
    Caching.privateCache(5.minutes, _)

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] =
    appMiddleware(cacheControMiddleware(routesMiddleware(http).orNotFound))

}
