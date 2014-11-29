package com.yiguang.mqtt

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.yiguang.mqtt.protocal.{QoSLevel, PUBLISH}
import com.yiguang.mqtt.provider.{PublishEvent, EventBus, Storage}

import scala.collection.concurrent.Map

/**
 * Created by yigli on 14-11-23.
 */

class DispatchActor(val clients: Map[String, ActorRef], val storage: Storage, val eventBus: EventBus)
  extends Actor with ActorLogging {

  def receive: Receive = {
    case "start" => eventBus.registerListener(dispatch _)
  }

  private[this] def dispatch(p: PublishEvent) = {
    val origin = storage.loadMessage(p.messageId)
    val subscribers = storage.loadSubscribers(p.topicName)

    for ((clientId, qos) <- subscribers) {
      val h = origin.header.qosLevel(if (qos < origin.header.qosLevel) qos else origin.header.qosLevel)
      val m = PUBLISH(h, origin.topicName, origin.messageId, origin.payload)


      clients.get(clientId) match {
        case Some(c) => c ! m
        case None => if (m.header.qosLevel > QoSLevel.AT_MOST_ONCE) {
          storage.saveFlightMessage(clientId, origin.id)
        }
      }
    }
  }

}

object DispatchActor {
  def props(clients: Map[String, ActorRef])
           (implicit storage: Storage, eventBus: EventBus) = Props(classOf[DispatchActor], clients,storage,eventBus)
}
