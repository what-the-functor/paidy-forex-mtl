package forex.domain

import java.time.OffsetDateTime
import io.circe.Decoder

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {

  /**
    * The current time with offset from GMT
    *
    * Do not use in production
    */
  def nowUnsafe: Timestamp =
    Timestamp(OffsetDateTime.now)

  implicit val timestampDecoder: Decoder[Timestamp] = Decoder[OffsetDateTime].map(Timestamp(_))
}
