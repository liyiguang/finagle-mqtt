package com.yiguang.mqtt.protocal

import org.jboss.netty.handler.codec.frame.FrameDecoder
import com.twitter.util.StateMachine
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}

/**
 * Created by roger on 3/14/14. 
 */
abstract class AbstractMqttMessageDecoder extends FrameDecoder with StateMachine {
  private[this] val needMoreData = null
  state = ReadHeader()

  case class ReadHeader() extends State

  case class ReadDataLength(header: MqttFixHeader, lengthDecoder: LengthDecoder) extends State

  case class ReadDate(header: MqttFixHeader, length: Int) extends State

  case class LengthDecoder(var length: Int, var multiplier: Int, var finished: Boolean) {
    def decode(in: ChannelBuffer): LengthDecoder = {
      while (in.readable()) {
        val digit = in.readByte()
        length = length + ((digit & 0x7F) * multiplier)
        if ((digit & 0x80) == 0) {
          return LengthDecoder(length, multiplier, true)
        }
        multiplier = multiplier << 7
      }
      LengthDecoder(length, multiplier, false)
    }
  }

  override def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): AnyRef = {
    state match {
      case ReadHeader() =>
        val header = new MqttFixHeader(buffer.readByte())
        state = ReadDataLength(header, LengthDecoder(0, 1, false))
        needMoreData
      case ReadDataLength(header, lengthDecoder) =>
        val ld = lengthDecoder.decode(buffer)
        if (ld.finished) {
          state = ReadDate(header, ld.length)
        } else {
          state = ReadDataLength(header, ld)
        }
        needMoreData
      case ReadDate(header, length) =>
        if (buffer.readableBytes() < length) {
          needMoreData
        } else {
          val data = ChannelBuffers.buffer(length)
          buffer.readBytes(data)
          state = ReadHeader()
          parseMQTTMessage(header, data)
        }
    }
  }

  def parseMQTTMessage(header: MqttFixHeader, body: ChannelBuffer): AnyRef
}
