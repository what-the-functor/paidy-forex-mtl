package forex.caches

import io.chrisdavenport.mules.Cache
import io.chrisdavenport.mules.http4s.CacheItem
import org.http4s.Method
import org.http4s.Uri

package object mules {
  type RatesCache[F[_]] = Cache[F, (Method, Uri), CacheItem]
}
