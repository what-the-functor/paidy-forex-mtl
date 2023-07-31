package forex.domain

import cats.Show
import cats.syntax.all._
import org.http4s.QueryParamEncoder

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
}
