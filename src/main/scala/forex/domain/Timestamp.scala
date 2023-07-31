package forex.domain

import java.time.OffsetDateTime

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {

  /**
   * The current time with offset from GMT
   *
   * Do not use in production
   */
  def nowUnsafe: Timestamp =
    Timestamp(OffsetDateTime.now)
}
