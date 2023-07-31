package forex.domain

import cats.Show
import io.circe.Decoder

import scala.collection.immutable.HashSet

sealed trait Currency

object Currency {
  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object NZD extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object USD extends Currency
  case object UNSUPPORTED extends Currency
  //case class UnknownCurrency(value: String) extends Currency

  implicit def show[A <: Currency]: Show[A] = Show.show {
    case AUD => "AUD"
    case CAD => "CAD"
    case CHF => "CHF"
    case EUR => "EUR"
    case GBP => "GBP"
    case NZD => "NZD"
    case JPY => "JPY"
    case SGD => "SGD"
    case USD => "USD"
    case UNSUPPORTED => "Unsupported currency"
  }

  def fromString(s: String): Currency = s.toUpperCase match {
    case "AUD" => AUD
    case "CAD" => CAD
    case "CHF" => CHF
    case "EUR" => EUR
    case "GBP" => GBP
    case "NZD" => NZD
    case "JPY" => JPY
    case "SGD" => SGD
    case "USD" => USD
    case _ => UNSUPPORTED
  }

  implicit val currencyDecoder: Decoder[Currency] = Decoder.decodeString.map(fromString)

  /** The set of all supported currencies */
  val all: HashSet[Currency] =
    HashSet(AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD)

  /** The set of all unique pairs (no currency paired with itself) */
  val allPairs: HashSet[Rate.Pair] =
    for {
      from <- all
      to <- all if (to != from)
      pair = Rate.Pair(from, to)
    } yield pair
}
