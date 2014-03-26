package com.twitter.finagle.mqtt.protocal

/**
 * Created by roger on 3/14/14.
 */
object MessageType {
  val CONNECT:Byte = 1
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

case class Topic(name:String,qos:Byte){
  var clienId:String = _
}

class MQTTHeader(val header:Byte) {
  def messageType = {
    ((header & 0xF0) >>> 4).asInstanceOf[Byte]
  }

  def qosLevel(qosLevel:Byte):MQTTHeader = {
    var byte = header & 0xF9
    byte = byte | ((qosLevel << 1) & 0x06)
    new MQTTHeader(byte.asInstanceOf[Byte])
  }

  def qosLevel = {
    ((header & 0x06) >>> 1).asInstanceOf[Byte]
  }

  def dup(dup:Boolean):MQTTHeader = {
    val byte = if(dup) header | 0x08 else header & 0xF7
    new MQTTHeader(byte.asInstanceOf[Byte])
  }

  def dup = {
    (header & 0x08) > 0
  }

  def retain(retain:Boolean):MQTTHeader = {
    val byte = if(retain) header | 0x01 else header & 0xFE
    new MQTTHeader(byte.asInstanceOf[Byte])
  }

  def retain = {
    (header & 0x01) > 0
  }

  override def toString():String = {
    Map(
      "messageType" -> messageType,
      "qos" -> qosLevel,
      "dup" -> dup,
      "retain" -> retain
    ).toString()
  }
}

object MQTTHeader {

  def apply(messageType:Byte,dupFlag:Boolean,qosLevel:Byte,retainFlag:Boolean):MQTTHeader = {

    //message type
    var header = (messageType << 4) & 0xF0
    //dup flag
    header = if(dupFlag) header | 0x08 else header & 0xF7
    //qos level
    header = header & 0xF9
    header = header | ((qosLevel << 1) & 0x06)
    //retain
    header = if(retainFlag) header | 0x01 else header & 0xFE

    new MQTTHeader(header.asInstanceOf[Byte])
  }

  val CONNECT = MQTTHeader(MessageType.CONNECT,false,0,false)
  val CONNACK = MQTTHeader(MessageType.CONNACK,false,0,false)
  val PUBLISH = MQTTHeader(MessageType.PUBLISH,false,0,false)
  val PUBACK  = MQTTHeader(MessageType.PUBACK,false,0,false)
  val PUBREC  = MQTTHeader(MessageType.PUBREC,false,0,false)
  val PUBREL  = MQTTHeader(MessageType.PUBREL,false,0,false)
  val PUBCOMP = MQTTHeader(MessageType.PUBCOMP,false,0,false)
  val SUBSCRIBE = MQTTHeader(MessageType.SUBSCRIBE,false,0,false)
  val SUBACK = MQTTHeader(MessageType.SUBACK,false,0,false)
  val UNSUBSCRIBE = MQTTHeader(MessageType.UNSUBSCRIBE,false,0,false)
  val UNSUBACK = MQTTHeader(MessageType.UNSUBACK,false,0,false)
  val PINGREQ = MQTTHeader(MessageType.PINGREQ,false,0,false)
  val PINGRESP = MQTTHeader(MessageType.PINGRESP,false,0,false)
  val DISCONNECT = MQTTHeader(MessageType.DISCONNECT,false,0,false)

}

abstract class MQTTMessage(val header: MQTTHeader)

case class CONNECT(override val header: MQTTHeader,
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
  password: String) extends MQTTMessage(header)

case class DISCONNECT() extends MQTTMessage(MQTTHeader.DISCONNECT)
case class PUBLISH(override val header:MQTTHeader,topicName: String,messageId: Int, payload:Array[Byte]) extends MQTTMessage(header){
  def this(topicName: String,messageId: Int, payload:Array[Byte]) = this(MQTTHeader.PUBLISH,topicName,messageId,payload)
}
case class SUBSCRIBE(override val header:MQTTHeader,messageId: Int, topics:List[Topic]) extends MQTTMessage(header)
case class UNSUBSCRIBE(override val header:MQTTHeader,messageId: Int,topics:List[String]) extends MQTTMessage(header)
case class PINGREQ(override val header:MQTTHeader) extends MQTTMessage(header)

case class CONNACK(returnCode: Byte) extends MQTTMessage(MQTTHeader.CONNACK)
case class PINGRESP() extends MQTTMessage(MQTTHeader.PINGRESP)
case class PUBACK(messageId:Int) extends MQTTMessage(MQTTHeader.PUBACK)
case class PUBCOMP(messageId:Int) extends MQTTMessage(MQTTHeader.PUBCOMP)
case class PUBREC(messageId:Int) extends MQTTMessage(MQTTHeader.PUBREC)
case class PUBREL(messageId:Int) extends MQTTMessage(MQTTHeader.PUBREL)
case class SUBACK(messageId:Int,qos: List[Byte]) extends MQTTMessage(MQTTHeader.SUBACK)
case class UNSUBACK(messageId:Int) extends MQTTMessage(MQTTHeader.UNSUBACK)
