package com.yiguang.mqtt.provider

import java.io.{ByteArrayInputStream, ObjectInputStream, ByteArrayOutputStream, ObjectOutputStream}

import scala.util.parsing.json.{JSON, JSONObject}

/**
 * Created by yigli on 14-11-18.
 */

abstract sealed class Event {

}

case class PublishEvent(clientId: String, topicName: String, messageId: String) extends Event {


  /*
  def toBytes() = {
    val bos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(bos)

    oos.writeUTF(clientId)
    oos.writeUTF(topicName)
    oos.writeUTF(messageId)
    oos.flush()

    bos.toByteArray
  }
  */


  def toJson() = {
    val json = JSONObject(
      Map[String, Any](
        "clientId" -> clientId,
        "messageId" -> messageId,
        "topicName" -> topicName
      )
    )
    json.toString()
  }
}

object PublishEvent {
  /*
  def fromBytes(bytes:Array[Byte]) = {
    val bis = new ByteArrayInputStream(bytes);
    val oin = new ObjectInputStream(bis)
    val clientId = oin.readUTF
    val topicName = oin.readUTF
    val messageId = oin.readUTF

    PublishEvent(clientId,topicName,messageId)
  }
  */

  def fromJson(json: String): Option[PublishEvent] = {
    val ret = JSON.parseFull(json)
    ret match {
      case Some(j) =>
        val map = j.asInstanceOf[Map[String, Any]]
        val clientId = map("clientId").asInstanceOf[String]
        val messageId = map("messageId").asInstanceOf[String]
        val topiName = map("topicName").asInstanceOf[String]
        Some(PublishEvent(clientId, topiName, messageId))
      case None =>
        None
    }
  }
}


abstract class EventBus {

  def fireEvent[E <: Event](e: E)

  def registerListener[E <: Event](f: E => Unit)

}
