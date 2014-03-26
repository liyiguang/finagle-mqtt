package com.twitter.finagle.mqtt.protocal

import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.slf4s.Logging
import com.twitter.finagle.mqtt.protocal.util.ChanelBufferUtils._

/**
 * Created by roger on 3/12/14. 
 */
class MQTTMessageEncoder extends OneToOneEncoder with Logging{

  def encodeLength(len:Int,out:ChannelBuffer) = {
    var remaining = len
    do{
      var digit = (remaining & 0x7F).asInstanceOf[Byte]
      remaining = remaining >>> 7
      if (remaining > 0) {
        digit = (digit | 0x80).asInstanceOf[Byte]
      }
      out.writeByte(digit)
    }while(remaining > 0)
  }
  override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: Any): AnyRef = {
    log.debug("Encode message:"+msg.toString)
    val body = ChannelBuffers.dynamicBuffer()
    val mqttMessage = msg.asInstanceOf[MQTTMessage]
    mqttMessage match {
      case s:CONNACK =>
        body.writeByte(0)
        body.writeByte(s.returnCode)
      case s:PUBLISH =>
        writeString(body,s.topicName)
        if(s.header.qosLevel == QoSLevel.AT_LEAST_ONCE ||
          s.header.qosLevel == QoSLevel.EXACTLY_ONCE){
          writeInt(body,s.messageId)
        }
        body.writeBytes(s.topicName)
      case s: PUBACK =>
        writeInt(body,s.messageId)
      case s:PUBREC =>
        writeInt(body,s.messageId)
      case s:PUBREL =>
        writeInt(body,s.messageId)
      case s:PUBCOMP =>
        writeInt(body,s.messageId)
      case s:SUBACK =>
        writeInt(body,s.messageId)
        s.qos.foreach(body.writeByte(_))
      case s:UNSUBACK =>
        writeInt(body,s.messageId)

    }

    val out = ChannelBuffers.dynamicBuffer()
    out.writeByte(mqttMessage.header.header)
    encodeLength(body.readableBytes(),out)
    out.writeBytes(body)

    out
  }
}
