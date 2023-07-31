package forex.services.rates.interpreters

import cats.data.EitherT
import cats.effect._
import cats.syntax.all._
import forex.config.OneFrameConfig
import forex.domain.Currency
import forex.domain.Rate
import forex.http.rates.Protocol._
import forex.services.rates.Algebra
import forex.services.rates.errors.Error
import org.http4s._
import org.http4s.client.Client

import scala.collection.immutable.HashSet
import scala.util.Try

class OneFrameHttp[F[_]: Sync](config: OneFrameConfig)(client: Client[F]) extends Algebra[F] {

  private def withOneFrameParams(baseUri: Uri): Set[Rate.Pair] => Uri =
    pairs => baseUri.withQueryParam("pair", pairs.toList)

  private val oneFrameUri: Set[Rate.Pair] => Uri =
    withOneFrameParams(config.ratesEndpoint)

  private def oneFrameRequest(pair: Set[Rate.Pair]): Request[F] =
    Request(Method.GET, oneFrameUri(pair), headers = Headers(Header("token", "10dc303535874aeccc86a8251e6992f5")))

  def get(pair: Rate.Pair): F[Error Either Rate] =
    (for {
      all <- EitherT(getAll(HashSet(pair)))

      errorOrRate = Try(all.head).toEither.leftMap(Error.unknownRateLookupFailure(pair))
      result <- EitherT(Sync[F].pure(errorOrRate))
    } yield result).value

  def getAll(pairs: Set[Rate.Pair]): F[Error Either HashSet[Rate]] =
    client.run(oneFrameRequest(Currency.allPairs)).use {
      case Status.Successful(body) =>
        body
          .attemptAs[List[GetApiResponse]]
          .map(HashSet.from)
          .map { xs =>
            xs.map(toRate)
          }
          .map(_.filter(rate => pairs.contains(rate.pair)))
          .leftMap(Error.rateDeserializationFailed)
          .value
      case resp => Sync[F].pure(Left(oneFrameLookupFailed(resp)))
    }

  val oneFrameLookupFailed: Response[F] => Error =
    failedResp => Error.OneFrameLookupFailed(s"One-Frame lookup failed with status: ${failedResp.status}")
}
