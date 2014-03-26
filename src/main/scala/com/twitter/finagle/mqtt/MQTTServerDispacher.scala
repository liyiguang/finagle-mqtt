package com.twitter.finagle.mqtt

import com.twitter.finagle.dispatch.GenSerialServerDispatcher
import com.twitter.finagle.mqtt.protocal.MQTTMessage
import com.twitter.finagle.transport.Transport
import com.twitter.util.Future
import com.twitter.finagle.Service
import io.netty.util.{DefaultAttributeMap, AttributeMap, Attribute, AttributeKey}

/**
 * Created by roger on 3/20/14. 
 */
class MQTTServerDispacher(
  val transport:Transport[MQTTMessage,MQTTMessage],
  service:Service[MQTTMessage, MQTTMessage])
  extends GenSerialServerDispatcher[MQTTMessage,MQTTMessage,MQTTMessage,MQTTMessage](transport)
  with Attributes {
  self =>

  override protected def dispatch(req: MQTTMessage): Future[MQTTMessage] = {
    service(new MQTTCommand(this,req))
  }

  //do nothing
  override protected def handle(rep: MQTTMessage): Future[Unit] = {Future.Unit}
}

class MQTTCommand(dispatcher:MQTTServerDispacher,val msg:MQTTMessage) extends MQTTMessage(msg.header) {
  def attr[T](key: AttributeKey[T]): Attribute[T] = dispatcher.attr(key)
  def transport = dispatcher.transport
}

trait Attributes extends AttributeMap {
  private val map = new DefaultAttributeMap
  override def attr[T](key: AttributeKey[T]): Attribute[T] = {
    map.attr(key)
  }
}





