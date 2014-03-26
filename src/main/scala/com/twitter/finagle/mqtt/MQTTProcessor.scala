package com.twitter.finagle.mqtt

import scala.actors.Actor
import com.twitter.finagle.mqtt.provider.{PubSub, Storage}
import com.twitter.finagle.mqtt.protocal._
import com.twitter.finagle.transport.Transport
import com.twitter.finagle.mqtt.protocal.PUBLISH
import com.twitter.finagle.mqtt.protocal.UNSUBSCRIBE
import com.twitter.finagle.mqtt.protocal.SUBSCRIBE
import org.slf4s.Logging

/**
 * Created by roger on 3/24/14. 
 */
class MQTTProcessor(trans:Transport[MQTTMessage,MQTTMessage])(implicit val storage:Storage,implicit val pubsub:PubSub) extends Actor with Logging {

  private def send(msg:MQTTMessage) = {
    trans.write(msg).onFailure { e =>
      log.error("Send Message Failed",e)
    }
  }

  override def act(): Unit = {
    loop {
      react {
        case DispatchInFlightMessage(clientId) =>
          var messageId = storage.getFromInbox(clientId)
          while (messageId != null) {
            val message0 = storage.load(messageId)
            val message = new PUBLISH(message0.topicName, messageId.toInt, message0.payload)
            send(message)
            messageId = storage.getFromInbox(clientId)
          }
        case CleanSession(clientId) =>
          storage.flushInbox(clientId)
          val topicNames = storage.unSubscribe(clientId)
          pubsub.unSubscribe(clientId,topicNames)

        case HandleRetainMessage(subscribe) =>
          handleRetainMessage(subscribe)

        case Subscribe(clientId,msg) =>
          storage.subscribe(clientId, msg.topics)
          handleRetainMessage(msg)
          pubsub.subscribe(clientId,msg.topics.map(_.name))

        case Unsubscribe(clientId,msg) =>
          storage.unSubscribe(clientId, msg.topics)
          pubsub.unSubscribe(clientId,msg.topics)

        case PublishMessage(msg) =>
          val messageId = storage.save(msg)
          if(msg.header.retain){
            storage.setTopicRetainMessage(msg.topicName,"")
          }else{
            storage.setTopicRetainMessage(msg.topicName,messageId)
          }
          pubsub.publish(msg.topicName,messageId)
      }
    }
  }

  def handleRetainMessage(subscribe:SUBSCRIBE){
    val lastMessageIds = storage.getTopicRetainMessages(subscribe.topics.map(_.name))
    for ((topic: Topic, messageId: String) <- subscribe.topics.zip(lastMessageIds)) {
      if (messageId != null) {
        val message0 = storage.load(messageId)
        val header = MQTTHeader.PUBLISH.qosLevel(math.min(topic.qos, message0.header.qosLevel).toByte).retain(true)
        val message = PUBLISH(header, topic.name, messageId.toInt, message0.payload)
        send(message)
      }
    }
  }
}

abstract sealed class ProcessorMessage
case class CleanSession(clientId:String) extends ProcessorMessage
case class DispatchInFlightMessage(clientId:String) extends ProcessorMessage
case class HandleRetainMessage(subscribe:SUBSCRIBE) extends ProcessorMessage
case class Subscribe(clientId:String,msg:SUBSCRIBE) extends ProcessorMessage
case class Unsubscribe(clientId:String,msg:UNSUBSCRIBE) extends ProcessorMessage
case class PublishMessage(msg:PUBLISH) extends ProcessorMessage
