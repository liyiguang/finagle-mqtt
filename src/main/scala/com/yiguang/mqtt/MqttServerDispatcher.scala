package com.yiguang.mqtt

import akka.actor.ActorRef
import com.twitter.finagle.Service
import com.twitter.finagle.dispatch.GenSerialServerDispatcher
import com.twitter.finagle.transport.Transport
import com.twitter.util.{Future, Promise, Throw}
import com.yiguang.mqtt.MqttServer.ServerManager.CreateSession
import com.yiguang.mqtt.protocal._
import org.slf4s.{Logging, Logger}


/**
 * one transport one dispatcher
 * @param transport
 * @param service
 * @param manager
 */
class MqttServerDispatcher(
                            val transport: Transport[Any, Any],
                            service: Service[MqttMessage, MqttMessage],
                            val manager: ActorRef)
  extends GenSerialServerDispatcher[MqttMessage, MqttMessage, Any, Any](transport) with Logging {

  private[this] var session: Option[Session] = None

  transport.onClose ensure {
    service.close()
  }

  protected def dispatch(req: Any, eos: Promise[Unit]): Future[MqttMessage] = {
    req match {
      case connect: CONNECT =>
        session = Some(Session(connect, transport))
        service(connect) ensure eos.setDone()
      case message: MqttMessage =>
        if (session == null) {
          return Future.exception(new Exception("Not Connected!"))
        }
        message.session = session
        service(message) ensure eos.setDone()
      case invalid =>
        eos.setDone()
        Future.exception(new IllegalArgumentException("Invalid message " + invalid))
    }
  }

  protected def handle(rep: MqttMessage): Future[Unit] = {
    if (rep == null) {
      return Future.Unit
    }

    rep match {
      case con@CONNACK(accept) =>
        if (accept == ConnectAckCode.ACCEPTED) {
          manager ! CreateSession(session.get)
          transport.write(rep)
        } else {
          session = None
          transport.write(rep) ensure transport.close()
        }
      case _ => transport.write(rep)
    }
  }

}

private[mqtt] case class Session(val clientId: String, val transport: Transport[Any, Any]) {
  var cleanSessionFlag: Boolean = _
  var willFLag: Boolean = _
  var willQosLeve: Byte = _
  var willRetainFlag: Boolean = _
  var keepAliveTimter: Int = _
  var willTopic: String = _
  var willMessage: String = _
  var lastActiveTime: Long = _

  def write(message: MqttMessage) = {
    transport.write(message)
  }

  def close = {
    transport.close()
  }
}

object Session {

  def apply(connect: CONNECT, transport: Transport[Any, Any]): Session = {
    val session = Session(connect.clientId, transport)
    session.cleanSessionFlag = connect.cleanSessionFlag
    session.willFLag = connect.willFlag
    session.willQosLeve = connect.willQosLevel
    session.willRetainFlag = connect.willRetainFlag
    session.willTopic = connect.willTopic
    session.willMessage = connect.willMessage
    session.keepAliveTimter = connect.keepAliveTime

    session
  }
}








