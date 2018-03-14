package circe.ccp.notification.util

import scala.annotation.tailrec

/**
 * Created by plt on 11/21/17.
 **/
object Random {

  def randomString(length: Int): String = {
    val r = new scala.util.Random
    val sb = new StringBuilder
    (1 to length) foreach sb.append(r.nextPrintableChar)
    sb.toString
  }
  def randomStringArray(length: Int): String = {
    val r = new scala.util.Random
    val a = new Array[Char](length)
    for (i <- 0 until length) {
      a(i) = r.nextPrintableChar
    }
    a.mkString
  }

  def randomStringRecursive(n: Int): List[Char] = n match {
    case 1 => List(util.Random.nextPrintableChar)
    case _ => List(util.Random.nextPrintableChar) ++ randomStringRecursive(n-1)
  }

  def randomStringRecursive2(n: Int): String = n match {
    case 1 => util.Random.nextPrintableChar.toString
    case _ => util.Random.nextPrintableChar.toString ++ randomStringRecursive2(n-1).toString
  }

  @tailrec
  def randomStringTailRecursive(n: Int, list: List[Char]):List[Char] = {
    if (n == 1) util.Random.nextPrintableChar :: list
    else randomStringTailRecursive(n-1, util.Random.nextPrintableChar :: list)
  }
  def randomStringRecursive2Wrapper(n: Int): String = randomStringTailRecursive(n, Nil).mkString

  def randomAlphaNumericString(length: Int): String = randomStringFromCharList(length, ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9'))

  def randomNumericString(length: Int): String = randomStringFromCharList(length, '0' to '9')

  def randomAlpha(length: Int): String = randomStringFromCharList(length, ('a' to 'z') ++ ('A' to 'Z'))

  def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    1 to length foreach(_ => {
      val randomNum = util.Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    })
    sb.toString
  }

  def genId: String = s"${System.currentTimeMillis()}_${randomNumericString(4)}"

}