package com.twitter.finagle.mqtt

import com.twitter.finagle.mqtt.protocal._
import com.twitter.finagle.Service
import com.twitter.util.Future
import org.slf4s.Logging
import java.util.concurrent.ConcurrentHashMap

import io.netty.util.AttributeKey
import com.twitter.finagle.mqtt.provider.{Storage, PubSub}
import com.twitter.finagle.mqtt.protocal.UNSUBACK
import com.twitter.finagle.mqtt.protocal.PINGREQ
import com.twitter.util.Throw
import com.twitter.finagle.mqtt.protocal.PINGRESP
import com.twitter.finagle.mqtt.protocal.DISCONNECT
import com.twitter.finagle.mqtt.protocal.PUBLISH
import com.twitter.finagle.mqtt.protocal.UNSUBSCRIBE
import com.twitter.finagle.mqtt.protocal.CONNECT
import com.twitter.finagle.mqtt.protocal.SUBACK
import com.twitter.finagle.mqtt.protocal.CONNACK
import com.twitter.finagle.mqtt.protocal.SUBSCRIBE

import MQTTModule._

/**
 * Created by roger on 3/14/14. 
 */
class MQTTService extends Service[MQTTMessage,MQTTMessage] with Logging {
  import ActiveClients._
  private val CLIENT_KEY: AttributeKey[Client] = AttributeKey.valueOf("MQTT_CLIENT")
  private val MAX_SUPPORTED_QOSLEVEL: Byte = QoSLevel.AT_LEAST_ONCE

  override def apply(request: MQTTMessage): Future[MQTTMessage] = {
    val command = request.asInstanceOf[MQTTCommand]
    val response = (msg:MQTTMessage) => {
      command.transport.write(msg).onFailure { e =>
        log.error("Response Failed",e)
      }
    }

    val client = {
      command.attr(CLIENT_KEY).get()
    }

    lazy val processor = (client:Client) => {
      var actor = clients.get(client.clientId)
      if(actor == null){
        actor = new MQTTProcessor(command.transport)
        clients.put(client.clientId,actor)
      }
      actor
    }

    lazy val close = command.transport.close()

    lazy val clientFactory = (msg:CONNECT) => {
      val client = Client(msg.clientId,msg.cleanSessionFlag,msg.willFlag,msg.willQosLevel,
        msg.willRetainFlag,msg.keepAliveTime,msg.willTopic,msg.willMessage)
      command.attr(CLIENT_KEY).set(client)
      command.transport.onClose ensure {
        log.debug("Remove client:"+client.clientId +" From active clients")
        clients.remove(client.clientId)
        command.attr(CLIENT_KEY).remove()
      }
      client
    }

    log.debug("Remote:"+command.transport.remoteAddress + "Message:"+command.msg)

    //process not connect command
    if(command.header.messageType != MessageType.CONNECT && client == null){
      log.error("Command Received before connected!")
      close
      return Future.value(null)
    }

    command.msg match {
      case msg : CONNECT =>
        val ackcode = if (msg.protocolName != "MQIsdp") {
          ConnectAckCode.UNACCEPTABLE_PROTOCOL_VERSION
        } else if (msg.protocolVersion != 3) {
          ConnectAckCode.UNACCEPTABLE_PROTOCOL_VERSION
        } else if (msg.clientId.size > 23) {
          ConnectAckCode.IDENTIFIER_REJECTED
        } else {
          ConnectAckCode.ACCEPTED
        }
        if(ackcode == ConnectAckCode.ACCEPTED){
          val connack = CONNACK(ackcode)
          response(connack)
          //create client session
          val client = clientFactory(msg)
          val actor = processor(client)
          //clean session
          if(client.cleanSessionFlag){
            actor ! CleanSession(client.clientId)
          }
          //dispach flight message
          actor ! DispatchInFlightMessage(client.clientId)
        }else{
          //TODO: need lean more
          close
        }
      case msg : PUBLISH =>
        msg.header.qosLevel match {
          case QoSLevel.AT_MOST_ONCE =>
          case QoSLevel.AT_LEAST_ONCE => response(PUBACK(msg.messageId))
          case QoSLevel.EXACTLY_ONCE => response(PUBREC(msg.messageId))
        }

        processor(client) ! PublishMessage(msg)

      case msg : SUBSCRIBE =>
        val suback = SUBACK(msg.messageId,msg.topics.map(
          s => if (s.qos > MAX_SUPPORTED_QOSLEVEL) MAX_SUPPORTED_QOSLEVEL else s.qos))
        response(suback)
        processor(client) ! Subscribe(client.clientId,msg)

      case msg : UNSUBSCRIBE =>
        val ack = UNSUBACK(msg.messageId)
        response(ack)
        processor(client) ! Unsubscribe(client.clientId,msg)

      case msg : DISCONNECT =>
        if(client.cleanSessionFlag){
          processor(client) ! CleanSession(client.clientId)
        }
        close

      case msg : PINGREQ =>
        response(PINGRESP())
      case _ => log.warn("Unsupport Message Received")

    }
    Future.value(null)
  }
}

object ActiveClients {
  val clients = new ConcurrentHashMap[String,MQTTProcessor]

  def client(clientId:String):Option[MQTTProcessor] = {
    val actor = clients.get(clientId)
    if(actor != null)
      Some(actor)
    else
      None
  }

}

private case class Client(clientId:String,cleanSessionFlag: Boolean,willFlag: Boolean,willQosLevel: Byte,
  willRetainFlag: Boolean,keepAliveTime: Int,willTopic: String,willMessage: String)


