package forex.http.rates

import cats._
import cats.implicits._
import forex.domain._
import io.circe._
import io.circe.parser._
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop._

import Protocol._

class ResponseDeserializationSuite extends ScalaCheckSuite {
  implicit val getApiResponseEq: Eq[GetApiResponse] = Eq.fromUniversalEquals

  implicit val currencyEq: Eq[Currency] = Eq.fromUniversalEquals

  val currencies: Gen[Currency] =
    Gen.oneOf(Currency.all)

  val pairs: Gen[Rate.Pair] =
    Gen.oneOf(Currency.allPairs)

  property("Raw currency deserializes") {
    forAll(currencies) { currency =>
      val json: Json = Json.fromString(currency.show)

      val result: io.circe.Error Either Currency = json.as[Currency]

      result === Right(currency)
    }
  }

  property("Wrapped currency deserializes") {
    case class Wrapper(from: Currency)

    implicit val wrappedEq: Eq[Wrapper] = Eq.fromUniversalEquals

    implicit val fromDecoder: Decoder[Wrapper] = Decoder.forProduct1("from")(Wrapper)

    forAll(currencies) { currency =>
      val json: Json = parse(s"""{"from":"${currency.show}"}""").getOrElse(Json.Null)

      val result: io.circe.Error Either Wrapper = json.as[Wrapper]

      result === Right(Wrapper(currency))
    }
  }

  property("One-frame response deserializes") {
    val price = Price(BigDecimal(100))

    val timestamp = Timestamp.nowUnsafe

    forAll(pairs) { pair =>
      val response: Json =
        parse(
          s"""[{"from":"${pair.from.show}","to":"${pair.to.show}","bid":0.7424694368031701,"ask":0.22505712244760834,"price":100,"time_stamp":"${timestamp.value.toString}"}]"""
        ).getOrElse(Json.Null)

      val result: io.circe.Error Either List[GetApiResponse] =
        response.as[List[GetApiResponse]]

      result === Right(List(GetApiResponse(pair.from, pair.to, price, timestamp)))
    }
  }
}
