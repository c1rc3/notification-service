package circe.ccp.notification.controller.http.filter

import com.twitter.finagle.http.filter.Cors.{HttpFilter, Policy}

/**
 * Created by plt on 11/28/17.
 **/

class CORSFilter extends HttpFilter(Policy(
  allowsOrigin = { origin => Some(origin) },
  allowsMethods = { _ => Some(Seq("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")) },
  allowsHeaders = { _ => Some(Seq("origin", "content-type", "accept", "authorization", "X-Requested-With", "X-Codingpedia", "cookie")) },
  supportsCredentials = true)) {
}