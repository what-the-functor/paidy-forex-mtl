package forex.programs.rates

import forex.services.rates.errors.{ Error => RatesServiceError }

object errors {

  sealed trait Error extends Exception
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameLookupFailed(msg) => Error.RateLookupFailed(s"Unknown error: $msg")
    case RatesServiceError.RateDeserializationFailed(msg) =>
      Error.RateLookupFailed(s"Unable to deserialize rate from upstream service. $msg")
    case RatesServiceError.UnknownRateLookupFailure(msg) =>
      Error.RateLookupFailed(msg)
  
    case _ => Error.RateLookupFailed("Unknown Error!!!")
  }
}
