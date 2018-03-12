package circe.ccp.notification.controller.http

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

/**
 * Created by phg on 3/12/18.
 **/
class PingController() extends Controller {
  get("/ping") {
    _: Request => response.ok("pong")
  }
}
