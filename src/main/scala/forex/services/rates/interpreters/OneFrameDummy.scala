package forex.services.rates.interpreters

import forex.services.rates.Algebra
import cats.Applicative
//import cats.syntax.all._
import cats.implicits._
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.rates.errors._
import scala.collection.immutable.HashSet

class OneFrameDummy[F[_]: Applicative] extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    Rate(pair, Price(BigDecimal(100)), Timestamp.nowUnsafe).asRight[Error].pure[F]
  
  override def getAll(pairs: Set[Rate.Pair]): F[Error Either HashSet[Rate]] =
    pairs
      .map(get)
      .toList
      .sequence
      .map(_.sequence)
      .map(_.map(HashSet.from))

}
