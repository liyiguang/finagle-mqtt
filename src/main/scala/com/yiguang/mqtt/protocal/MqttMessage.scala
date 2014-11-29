package com.yiguang.mqtt.protocal

import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}

import com.yiguang.mqtt.Session

/**
 * Created by roger on 3/14/14.
 */
object MessageType {
  val CONNECT: Byte = 1
  val CONNACK: Byte = 2
  val PUBLISH: Byte = 3
  val PUBACK: Byte = 4
  val PUBREC: Byte = 5
  val PUBREL: Byte = 6
  val PUBCOMP: Byte = 7
  val SUBSCRIBE: Byte = 8
  val SUBACK: Byte = 9
  val UNSUBSCRIBE: Byte = 10
  val UNSUBACK: Byte = 11
  val PINGREQ: Byte = 12
  val PINGRESP: Byte = 13
  val DISCONNECT: Byte = 14
}

object QoSLevel {
  val AT_MOST_ONCE: Byte = 0
  val AT_LEAST_ONCE: Byte = 1
  val EXACTLY_ONCE: Byte = 2
}

object ConnectAckCode {
  val ACCEPTED: Byte = 0
  val UNACCEPTABLE_PROTOCOL_VERSION: Byte = 1
  val IDENTIFIER_REJECTED: Byte = 2
  val SERVER_UNAVAILABLE: Byte = 3
  val BAD_USERNAME_OR_PASSWORD: Byte = 4
  val NOT_AUTHORIZED: Byte = 5
}

class MqttFixHeader(val header: Byte) {
  def messageType = {
    ((header & 0xF0) >>> 4).asInstanceOf[Byte]
  }

  def qosLevel(qosLevel: Byte): MqttFixHeader = {
    var byte = header & 0xF9
    byte = byte | ((qosLevel << 1) & 0x06)
    new MqttFixHeader(byte.asInstanceOf[Byte])
  }

  def qosLevel = {
    ((header & 0x06) >>> 1).asInstanceOf[Byte]
  }

  def dup(dup: Boolean): MqttFixHeader = {
    val byte = if (dup) header | 0x08 else header & 0xF7
    new MqttFixHeader(byte.asInstanceOf[Byte])
  }

  def dup = {
    (header & 0x08) > 0
  }

  def retain(retain: Boolean): MqttFixHeader = {
    val byte = if (retain) header | 0x01 else header & 0xFE
    new MqttFixHeader(byte.asInstanceOf[Byte])
  }

  def retain = {
    (header & 0x01) > 0
  }

  override def toString(): String = {
    Map(
      "messageType" -> messageType,
      "qos" -> qosLevel,
      "dup" -> dup,
      "retain" -> retain
    ).toString()
  }
}

object MqttFixHeader {

  def apply(header: Byte) = {
    new MqttFixHeader(header)
  }

  def apply(messageType: Byte, dupFlag: Boolean, qosLevel: Byte, retainFlag: Boolean): MqttFixHeader = {

    //message type
    var header = (messageType << 4) & 0xF0
    //dup flag
    header = if (dupFlag) header | 0x08 else header & 0xF7
    //qos level
    header = header & 0xF9
    header = header | ((qosLevel << 1) & 0x06)
    //retain
    header = if (retainFlag) header | 0x01 else header & 0xFE

    new MqttFixHeader(header.asInstanceOf[Byte])
  }

  val CONNECT = MqttFixHeader(MessageType.CONNECT, false, 0, false)
  val CONNACK = MqttFixHeader(MessageType.CONNACK, false, 0, false)
  val PUBLISH = MqttFixHeader(MessageType.PUBLISH, false, 0, false)
  val PUBACK = MqttFixHeader(MessageType.PUBACK, false, 0, false)
  val PUBREC = MqttFixHeader(MessageType.PUBREC, false, 0, false)
  val PUBREL = MqttFixHeader(MessageType.PUBREL, false, 0, false)
  val PUBCOMP = MqttFixHeader(MessageType.PUBCOMP, false, 0, false)
  val SUBSCRIBE = MqttFixHeader(MessageType.SUBSCRIBE, false, 0, false)
  val SUBACK = MqttFixHeader(MessageType.SUBACK, false, 0, false)
  val UNSUBSCRIBE = MqttFixHeader(MessageType.UNSUBSCRIBE, false, 0, false)
  val UNSUBACK = MqttFixHeader(MessageType.UNSUBACK, false, 0, false)
  val PINGREQ = MqttFixHeader(MessageType.PINGREQ, false, 0, false)
  val PINGRESP = MqttFixHeader(MessageType.PINGRESP, false, 0, false)
  val DISCONNECT = MqttFixHeader(MessageType.DISCONNECT, false, 0, false)

}

abstract sealed class MqttMessage(val header: MqttFixHeader) {
  var session: Option[Session] = None
}

case class CONNECT(override val header: MqttFixHeader,
                   protocolName: String,
                   protocolVersion: Byte,
                   cleanSessionFlag: Boolean,
                   willFlag: Boolean,
                   willQosLevel: Byte,
                   willRetainFlag: Boolean,
                   passwordFlag: Boolean,
                   userNameFlag: Boolean,
                   keepAliveTime: Int,
                   clientId: String,
                   willTopic: String,
                   willMessage: String,
                   userName: String,
                   password: String) extends MqttMessage(header)

case class DISCONNECT() extends MqttMessage(MqttFixHeader.DISCONNECT)

case class PUBLISH(override val header: MqttFixHeader, topicName: String, messageId: Int, payload: Array[Byte])
  extends MqttMessage(header) {

  //for storage
  var id: String = _

  def this(topicName: String, messageId: Int, payload: Array[Byte]) =
    this(MqttFixHeader.PUBLISH, topicName, messageId, payload)

  def toBytes = {
    val bos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(bos)
    oos.writeByte(header.header)
    oos.writeUTF(id)
    oos.writeUTF(topicName)
    oos.writeInt(messageId)
    oos.writeInt(payload.length)
    oos.write(payload)
    oos.flush()

    bos.toByteArray
  }
}

object PUBLISH {
  def apply(bytes: Array[Byte]): PUBLISH = {
    val bis = new ByteArrayInputStream(bytes);
    val oin = new ObjectInputStream(bis)

    val fixHeader = oin.readByte()
    val id = oin.readUTF()

    val topicName = oin.readUTF()
    val messageId = oin.readInt()

    val payloadLength = oin.readInt();
    val payload = new Array[Byte](payloadLength)
    oin.read(payload)

    val p = PUBLISH(MqttFixHeader(fixHeader), topicName, messageId, payload)
    p.id = id

    p
  }
}

case class SUBSCRIBE(override val header: MqttFixHeader, messageId: Int, topics: List[(String, Byte)]) extends MqttMessage(header)

case class UNSUBSCRIBE(override val header: MqttFixHeader, messageId: Int, topics: List[String]) extends MqttMessage(header)

case class PINGREQ(override val header: MqttFixHeader) extends MqttMessage(header)

case class CONNACK(val returnCode: Byte) extends MqttMessage(MqttFixHeader.CONNACK)

case class PINGRESP() extends MqttMessage(MqttFixHeader.PINGRESP)

case class PUBACK(messageId: Int) extends MqttMessage(MqttFixHeader.PUBACK)

case class PUBCOMP(messageId: Int) extends MqttMessage(MqttFixHeader.PUBCOMP)

case class PUBREC(messageId: Int) extends MqttMessage(MqttFixHeader.PUBREC)

case class PUBREL(messageId: Int) extends MqttMessage(MqttFixHeader.PUBREL)

case class SUBACK(messageId: Int, qos: List[Byte]) extends MqttMessage(MqttFixHeader.SUBACK)

case class UNSUBACK(messageId: Int) extends MqttMessage(MqttFixHeader.UNSUBACK)




