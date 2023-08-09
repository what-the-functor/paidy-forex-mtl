package forex.http
package rates

import cats.effect.Sync
import forex.domain.Currency._
import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import org.http4s.EntityDecoder
import org.http4s.circe._

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class GetApiRequest(
      from: Currency,
      to: Currency
  )

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  def toRate(r: GetApiResponse) =
    Rate(
      Rate.Pair(r.from, r.to),
      r.price,
      r.timestamp
    )

  implicit val responseDecoder: Decoder[GetApiResponse] =
    Decoder.forProduct4("from", "to", "price", "time_stamp")(GetApiResponse)

  implicit def responseEntityDecoder[F[_]: Sync]: EntityDecoder[F, List[GetApiResponse]] =
    jsonOf[F, List[GetApiResponse]]

  implicit val currencyEncoder: Encoder[Currency] =
    Encoder.instance[Currency] { show.show _ andThen Json.fromString }

  implicit val pairEncoder: Encoder[Pair] =
    deriveConfiguredEncoder[Pair]

  implicit val rateEncoder: Encoder[Rate] =
    deriveConfiguredEncoder[Rate]

  implicit val responseEncoder: Encoder[GetApiResponse] =
    deriveConfiguredEncoder[GetApiResponse]

}
