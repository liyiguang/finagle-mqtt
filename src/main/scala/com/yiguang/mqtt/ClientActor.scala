package com.yiguang.mqtt

import akka.actor.{Props, Actor, ActorLogging}
import com.yiguang.mqtt.ClientActor.DispatchInFlightMessage
import com.yiguang.mqtt.ClientActor.Stop
import com.yiguang.mqtt.protocal._
import com.yiguang.mqtt.provider.{PublishEvent, Storage, EventBus}
import com.yiguang.util.StringUtils._

/**
 * Use for push message to client
 */
class ClientActor(val session: Session, val storage: Storage, val eventBus: EventBus) extends Actor with ActorLogging {

  private[this] val WAIT_ACKS = scala.collection.mutable.Map[Int, Long]()

  override def receive: Receive = {
    case DispatchInFlightMessage => dipatchInflightMessage
    case Stop(force) =>
      //stop actor first
      context.stop(self)

      //send will message if need
      if (force && session.willFLag) {
        sendWillMessage
      }

      //close the socket
      session.close

    case p: PUBLISH =>
      session.write(p) onFailure { x =>
        log.error("Push message failed,message=" + p, x)
        if (p.header.qosLevel > QoSLevel.AT_MOST_ONCE) {
          storage.saveFlightMessage(session.clientId, p.id)
        }
        session.close
      }

      //qosLeve = 1 need ack
      if (p.header.qosLevel == QoSLevel.AT_LEAST_ONCE) {
        WAIT_ACKS.put(p.messageId, System.currentTimeMillis())
      }
    case PUBACK(messageId) =>
      WAIT_ACKS.remove(messageId)

    case m =>
      log.debug("Unknown Message:" + m)
  }


  def sendWillMessage = {
    val publish = PUBLISH(MqttFixHeader.PUBLISH, session.willTopic, storage.genMessageId(), session.willMessage)
    val id = storage.saveMessage(publish)
    eventBus.fireEvent(PublishEvent(session.clientId, publish.topicName, id))
  }

  def dipatchInflightMessage(): Unit = {
    var messageId = storage.deleteFromFlightMessageBox(session.clientId)
    while (messageId != null) {
      val p = storage.loadMessage(messageId)
      if (p == null) {
        log.warning("Lost message id=" + messageId)
      } else {
        session.write(p) onFailure { x =>
          log.error("Dispatch inflight message:" + p + " failed", x)
          session.close
          return
        }
      }

      messageId = storage.deleteFromFlightMessageBox(session.clientId)
    }
  }
}

object ClientActor {
  def props(session: Session)(implicit storage: Storage, eventBus: EventBus)
  = Props(classOf[ClientActor])

  final case class DispatchInFlightMessage()

  final case class Stop(val force: Boolean = false)

}


