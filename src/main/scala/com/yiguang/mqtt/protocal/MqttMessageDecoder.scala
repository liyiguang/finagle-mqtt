package com.yiguang.mqtt.protocal

import org.jboss.netty.buffer.ChannelBuffer
import com.yiguang.mqtt.protocal.util.ChanelBufferUtils._
import org.slf4s.Logging

/**
 * Created by roger on 3/12/14. 
 */

class MqttMessageDecoder extends AbstractMqttMessageDecoder with Logging {

  def parseMQTTMessage(header: MqttFixHeader, body: ChannelBuffer): AnyRef = {
    val m = header.messageType match {
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
        CONNECT(header, protocolName, protocolVersion, cleanSessionFlag,
          willFlag: Boolean, willQos, willRetainFlag, passwordFlag,
          usernameFlag, keepAliveTime: Int, clientId, willTopic,
          willMessage, userName, password)
      case MessageType.DISCONNECT =>
        DISCONNECT()
      case MessageType.PUBLISH =>
        val topicName = readString(body)
        val messageId = if (header.qosLevel == QoSLevel.AT_LEAST_ONCE
          || header.qosLevel == QoSLevel.EXACTLY_ONCE) readInt(body)
        else 0
        val payload = new Array[Byte](body.readableBytes())
        body.readBytes(payload)
        PUBLISH(header, topicName, messageId, payload)
      case MessageType.SUBSCRIBE =>
        val messageId = readInt(body)
        var topics = List[(String, Byte)]()
        while (body.readableBytes() > 0) {
          val name = readString(body)
          val qos = body.readByte()
          topics = topics :+(name, qos)
        }
        SUBSCRIBE(header, messageId, topics)
      case MessageType.UNSUBSCRIBE =>
        val messageId = readInt(body)
        var topicNames = List[String]()
        while (body.readableBytes() > 0) {
          val topicName = readString(body)
          topicNames = topicName :: topicNames
        }
        UNSUBSCRIBE(header, messageId, topicNames.reverse)
      case MessageType.PINGREQ =>
        PINGREQ(header)
      case MessageType.PUBACK =>
        val messageId = readInt(body)
        PUBACK(messageId)

      case _ =>
        log.error("Unsupported header type:" + header.messageType)
        null
    }
    log.debug("Message Received:"+m)
    m
  }
}

