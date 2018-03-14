package circe.ccp.notification.util

import com.twitter.inject.Logging
import com.twitter.util.NonFatal

/**
 * Created by phg on 12/27/17.
 **/
trait Cronning extends Logging {
  var threads: Seq[Thread] = Seq[Thread]()
  def run(period: Long, name: String = "")(fn: => Unit): Unit = {
    val thread = new Thread(new Runnable() {
      override def run() = while (true) try {
        fn
        if (period > 0) Thread.sleep(period)
      } catch {
        case NonFatal(throwable) => error(s"[ERROR] Cronning.run", throwable)
      }
    })
    thread.setName(name)
    thread.start()
    threads = threads :+ thread
  }
}
