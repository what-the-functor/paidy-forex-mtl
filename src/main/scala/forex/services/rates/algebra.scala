package forex.services.rates

import forex.domain.Rate
import errors._
import scala.collection.immutable.HashSet

trait Algebra[F[_]] {

  /** Retrieve a single rate */
  def get(pair: Rate.Pair): F[Error Either Rate]
  
  /** Retrieve a batch of rates */
  def getAll(pairs: Set[Rate.Pair]): F[Error Either HashSet[Rate]]
}
