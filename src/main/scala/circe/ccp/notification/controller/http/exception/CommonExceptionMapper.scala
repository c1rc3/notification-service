package circe.ccp.notification.controller.http.exception

import javax.inject.Inject

import circe.ccp.notification.domain.StandardResponse
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.Logging

/**
 * Created by plt on 11/16/17.
 **/
class CommonExceptionMapper @Inject()(response: ResponseBuilder) extends ExceptionMapper[Exception] with Logging {
  override def toResponse(request: Request, throwable: Exception): Response = {
    throwable match {
      case s@_ =>
        s.printStackTrace()
        error("[ERROR]CommonExceptionMapper.toResponse", s)
        response.internalServerError(StandardResponse(code = -1, msg = s"${s.getClass.getName}: ${s.getLocalizedMessage}"))
    }
  }
}
