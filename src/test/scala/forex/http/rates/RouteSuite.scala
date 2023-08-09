package forex.http.rates

import cats.data.OptionT
import cats.effect.IO
import cats.syntax.all._
import forex.domain.Currency
import forex.domain.Rate
import forex.programs.RatesProgram
import forex.services.RatesServices
import munit.ScalaCheckSuite
import org.http4s.HttpRoutes
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.scalacheck.Gen
import org.scalacheck.Prop._

class RouteSuite extends ScalaCheckSuite {
  val routes: HttpRoutes[IO] =
    new RatesHttpRoutes[IO](RatesProgram(RatesServices.dummy)).routes

  val matchingPairs: Gen[Rate.Pair] =
    Gen.oneOf(Currency.all).map(x => Rate.Pair(x, x))

  val uniquePairs: Gen[Rate.Pair] =
    Gen.oneOf(Currency.allPairs)

  val uri: Rate.Pair => Uri = pair => Uri.unsafeFromString(s"/rates?from=${pair.from.show}&to=${pair.to.show}")

  val request: Rate.Pair => Request[IO] =
    uri andThen (Request(Method.GET, _))

  property("Matched pairs are not routed") {
    forAll(matchingPairs) { pair =>
      val response: OptionT[IO, Response[IO]] =
        routes.run(request(pair))

      response.isEmpty.unsafeRunSync()
    }
  }

  property("Unique pairs are routed") {
    forAll(uniquePairs) { pair =>
      val response: OptionT[IO, Response[IO]] =
        routes.run(request(pair))

      response.exists(_.status.isSuccess).unsafeRunSync()
    }
  }
}
