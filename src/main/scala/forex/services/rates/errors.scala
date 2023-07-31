package forex
package services.rates

import domain.Rate
import org.http4s.DecodeFailure
import cats.syntax.show._

object errors {

  sealed trait Error
  object Error {
    final case class OneFrameLookupFailed(msg: String) extends Error
    final case class RateDeserializationFailed(e: DecodeFailure) extends Error
    final case class UnknownRateLookupFailure(msg: String) extends Error

    def unknownRateLookupFailure(pair: Rate.Pair): Throwable => Error = e =>
      UnknownRateLookupFailure(s"Unknown rate lookup failure for ${pair.show}:\n${e.getMessage}")

    def rateDeserializationFailed(e: DecodeFailure): Error =
      RateDeserializationFailed(e)
  }

}
