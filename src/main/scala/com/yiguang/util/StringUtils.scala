package com.yiguang.util

import java.nio.charset.Charset

/**
 * Created by yigli on 14-11-23.
 */
object StringUtils {

  val UTF8 = Charset.forName("UTF-8")

  implicit def toBytes(str: String) = str.getBytes(UTF8)

  implicit def fromBytes(bytes: Array[Byte]) = new String(bytes, UTF8)
}
