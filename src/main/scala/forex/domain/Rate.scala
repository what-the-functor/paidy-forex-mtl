package forex.domain

import cats.Show
import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.QueryParamEncoder
import org.http4s.circe._

import scala.collection.immutable.HashSet

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  implicit def showPair(implicit showCurrency: Show[Currency]): Show[Pair] =
    Show.show { case Pair(c1, c2) => s"${c1.show}${c2.show}" }

  implicit val pairQueryParamEncoder: QueryParamEncoder[Pair] =
    QueryParamEncoder.fromShow

  implicit def ratesEntityDecoder[F[_]: Sync]: EntityDecoder[F, HashSet[Rate]] = jsonOf[F, HashSet[Rate]]

  implicit def rateEntityDecoder[F[_]: Sync]: EntityDecoder[F, Rate] = jsonOf[F, Rate]
}
