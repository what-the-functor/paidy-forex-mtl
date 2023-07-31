package forex

import cats.effect._
import forex.config._
import fs2.Stream
import io.chrisdavenport.mules.MemoryCache
import io.chrisdavenport.mules.TimeSpec
import io.chrisdavenport.mules.http4s.CacheItem
import io.chrisdavenport.mules.http4s.CacheMiddleware
import io.chrisdavenport.mules.http4s.CacheType
import org.http4s.Method
import org.http4s.Uri
import org.http4s.client.blaze._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)
}

class Application[F[_]: ConcurrentEffect: Timer] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      ttl = TimeSpec.fromDuration(config.cache.ttl)
      cache <- Stream.eval {
                MemoryCache
                  .ofSingleImmutableMap[F, (Method, Uri), CacheItem](ttl)
              }
      withCache = CacheMiddleware.client(cache, CacheType.Public)
      client <- BlazeClientBuilder[F](ec).stream
      module = new Module[F](withCache(client), config)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
