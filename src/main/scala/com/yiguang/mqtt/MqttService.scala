package com.yiguang.mqtt

import akka.actor.ActorRef
import com.twitter.finagle.Service
import com.twitter.util.Future
import com.yiguang.mqtt.ClientActor.Stop
import com.yiguang.mqtt.protocal._
import com.yiguang.mqtt.provider.{EventBus, PublishEvent, Storage}
import org.slf4s.Logging

import scala.collection.concurrent.Map

/**
 * Created by yigli on 14-11-20.
 */
class MqttService(val clients: Map[String, ActorRef])(implicit val eventBus: EventBus, implicit val storage: Storage)
  extends Service[MqttMessage, MqttMessage] with Logging {
  private[this] val NOMESSAGE:Future[MqttMessage] = Future.value(null)

  //currently only support qos=0 or 1
  private val maxQos = QoSLevel.AT_LEAST_ONCE

  override def apply(request: MqttMessage): Future[MqttMessage] = {
    request match {
      case m: CONNECT => Future.value(processConnect(m))
      case m: SUBSCRIBE => processSubscribe(m)
      case m: UNSUBSCRIBE => Future.value(processUnSubscribe(m))
      case m: PUBLISH => processPublish(m)
      case m: PINGREQ => Future.value(PINGRESP())
      case m: DISCONNECT => processDisconnect(m)
      case m: PUBACK => processPubAck(m)

      case _ =>
        log.warn("Unsupported Message Received" + request)
        Future.exception(new IllegalArgumentException())
    }
  }

  private[this] def processConnect(connect: CONNECT): CONNACK = {
    val clientId = connect.clientId

    /**
     * close the old session if exist
     * 1. remove from the client actor from clients
     * 2. stop the actor
     * 3. close the transport
     */
    clients.remove(clientId) match {
      case Some(client) => client ! Stop
    }

    val ac = if (connect.protocolName != "MQIsdp") {
      ConnectAckCode.UNACCEPTABLE_PROTOCOL_VERSION
    } else if (connect.protocolVersion != 3) {
      ConnectAckCode.UNACCEPTABLE_PROTOCOL_VERSION
    } else if (connect.clientId.size > 23) {
      ConnectAckCode.IDENTIFIER_REJECTED
    } else {
      ConnectAckCode.ACCEPTED
    }
    val connack = CONNACK(ac)

    if (ac != ConnectAckCode.ACCEPTED) return connack

    if (connect.cleanSessionFlag) {
      cleanSession(clientId)
    }

    connack
  }

  private[this] def cleanSession(clientId: String) = {
    storage.deleteFlightMessageBox(clientId)
    storage.deleteSubscribes(clientId)
  }

  private[this] def processSubscribe(sub: SUBSCRIBE): Future[MqttMessage] = {
    val session = sub.session.get
    val topcis = sub.topics.map(x => if (x._2 > maxQos) (x._1, maxQos) else x)

    val subAck = SUBACK(sub.messageId, topcis.map(_._2))

    session.write(subAck)
    dispatchRetainMessage(session, sub.topics)

    //storge the subscribes and dispatcher will dispatch the message subscribed topic
    storage.saveSubscribes(session.clientId, topcis)

    NOMESSAGE
  }

  private[this] def dispatchRetainMessage(session: Session, topics: List[(String, Byte)]) = {
    val retains = storage.loadRetainMessages(topics.map(_._1))

    for ((_, id) <- retains) {
      val m = storage.loadMessage(id)
      //qosLevel process
      session.write(m)
    }
  }

  private[this] def processUnSubscribe(unSub: UNSUBSCRIBE): UNSUBACK = {
    val session = unSub.session.get
    storage.deleteSubscribes(session.clientId, unSub.topics)
    UNSUBACK(unSub.messageId)
  }

  private[this] def processPublish(pub: PUBLISH): Future[PUBACK] = {

    if (pub.header.qosLevel > maxQos) {
      Future.exception(new Exception("Unsupport Qos Level"));
    }

    val id = storage.saveMessage(pub)

    if (pub.header.retain) {
      storage.updateRetainMessage(pub.topicName, id)
    }
    //send to event bus
    val clientId = pub.session.get.clientId
    eventBus.fireEvent(PublishEvent(clientId, pub.topicName, id))

    Future.value(PUBACK(pub.messageId))
  }

  private[this] def processDisconnect(disConnect: DISCONNECT): Future[MqttMessage] = {
    // stop client actor
    // clean session
    val clientId = disConnect.session.get.clientId
    cleanSession(clientId)

    clients.remove(clientId) match {
      case Some(client) => client ! Stop
    }

    NOMESSAGE
  }

  private[this] def processPubAck(pubAck: PUBACK): Future[MqttMessage] = {
    val clientId = pubAck.session.get.clientId
    clients.get(clientId) match {
      case Some(client) => client ! pubAck
    }

    NOMESSAGE
  }

}
