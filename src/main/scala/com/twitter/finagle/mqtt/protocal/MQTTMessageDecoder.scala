package com.twitter.finagle.mqtt.protocal

import org.jboss.netty.buffer.ChannelBuffer
import com.twitter.finagle.mqtt.protocal.util.ChanelBufferUtils._
/**
 * Created by roger on 3/12/14. 
 */

class MQTTMessageDecoder extends AbstractMQTTMessageDecoder {

  def parseMQTTMessage(header:MQTTHeader,body:ChannelBuffer):AnyRef = {
    header.messageType match {
      case MessageType.CONNECT =>
        val protocolName = readString(body)
        val protocolVersion = (body.readByte() & 0xFF).asInstanceOf[Byte]
        val flags = body.readByte()
        val usernameFlag = (flags & 0x80) > 0
        val passwordFlag = (flags & 0x40) > 0
        val willRetainFlag = (flags & 0x20) > 0
        val willQos = ((flags & 0x18) >>> 3).asInstanceOf[Byte]
        val willFlag = (flags & 0x04) > 0
        val cleanSessionFlag = (flags & 0x02) > 0
        val keepAliveTime = readInt(body)
        val clientId = readString(body)
        val willTopic = if (willFlag) readString(body) else ""
        val willMessage = if (willFlag) readString(body) else ""
        val userName = if (usernameFlag) readString(body) else ""
        val password = if (passwordFlag) readString(body) else ""
        CONNECT(header,protocolName,protocolVersion,cleanSessionFlag,
          willFlag:Boolean,willQos,willRetainFlag,passwordFlag,
          usernameFlag,keepAliveTime:Int,clientId,willTopic,
          willMessage,userName,password)
      case MessageType.DISCONNECT =>
        DISCONNECT()
      case MessageType.PUBLISH =>
        val topicName = readString(body)
        val messageId = if (header.qosLevel == QoSLevel.AT_LEAST_ONCE
          || header.qosLevel == QoSLevel.EXACTLY_ONCE) readInt(body) else 0
        val payload = new Array[Byte](body.readableBytes())
        body.readBytes(payload)
        PUBLISH(header,topicName,messageId,payload)
      case MessageType.SUBSCRIBE =>
        val messageId = readInt(body)
        var topics = List[Topic]()
        while (body.readableBytes() > 0) {
          val name = readString(body)
          val qos = body.readByte()
          topics = Topic(name,qos) :: topics
        }
        SUBSCRIBE(header,messageId,topics.reverse)
      case MessageType.UNSUBSCRIBE =>
        val messageId = readInt(body)
        var topicNames = List[String]()
        while (body.readableBytes() > 0) {
          val topicName = readString(body)
          topicNames = topicName :: topicNames
        }
        UNSUBSCRIBE(header,messageId,topicNames.reverse)
      case MessageType.PINGREQ =>
        PINGREQ(header)
    }
  }
}

