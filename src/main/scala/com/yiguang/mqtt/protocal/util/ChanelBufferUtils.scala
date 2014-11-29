package com.yiguang.mqtt.protocal.util

import com.google.common.base.Strings
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.util.CharsetUtil

/**
 * Created by roger on 3/13/14. 
 */
object ChanelBufferUtils {
  implicit def stringToChannelBuffer(string: String) =
    if (Strings.isNullOrEmpty(string)) null
    else {
      ChannelBuffers.copiedBuffer(string, CharsetUtil.UTF_8)
    }

  def channelBufferToString(channelBuffer: ChannelBuffer): String =
    new String(channelBufferToBytes(channelBuffer), CharsetUtil.UTF_8)

  def channelBufferToBytes(channelBuffer: ChannelBuffer): Array[Byte] = {
    val length = channelBuffer.readableBytes()
    val bytes = new Array[Byte](length)
    channelBuffer.getBytes(channelBuffer.readerIndex(), bytes, 0, length)
    bytes
  }

  def readString(buffer: ChannelBuffer): String = {
    if (buffer.readableBytes() < 2) {
      return null
    } else {
      val len = buffer.readUnsignedShort()
      if (buffer.readableBytes() < len) {
        return null
      }
      val bytes = new Array[Byte](len)
      buffer.readBytes(bytes)
      return new String(bytes, CharsetUtil.UTF_8)
    }
  }

  def readInt(buffer: ChannelBuffer): Int = {
    return buffer.readUnsignedShort()
  }

  def writeInt(out: ChannelBuffer, int: Int): Unit = out.writeShort(int)

  def writeString(out: ChannelBuffer, str: String) = {
    val raw: Array[Byte] = str.getBytes(CharsetUtil.UTF_8)
    out.writeShort(raw.length)
    out.writeBytes(raw)
  }
}
