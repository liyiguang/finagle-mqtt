package com.yiguang.mqtt

import akka.actor.ActorRef
import com.twitter.finagle.transport.Transport
import com.twitter.finagle.{Codec, CodecFactory, Service}
import com.twitter.util.Closable
import com.yiguang.mqtt.protocal._
import org.jboss.netty.channel.{ChannelPipelineFactory, Channels}

/**
 * Created by roger on 3/13/14. 
 */

object MqttCodec {
  def apply(manager: ActorRef) = new MqttCodec(manager)
}


class MqttCodec(val manager: ActorRef) extends CodecFactory[MqttMessage, MqttMessage] {

  def server = Function.const {

    new Codec[MqttMessage, MqttMessage] {
      def pipelineFactory = MqttServerPipelineFactory

      override def newServerDispatcher(transport: Transport[Any, Any], service: Service[MqttMessage, MqttMessage]): Closable = {
        new MqttServerDispatcher(transport, service, manager)
      }
    }
  }

  def client = Function.const {
    new Codec[MqttMessage, MqttMessage] {
      def pipelineFactory = MqttClientPipelineFactory
    }
  }
}

object MqttClientPipelineFactory extends ChannelPipelineFactory {
  def getPipeline() = {
    val pipeline = Channels.pipeline()

    pipeline.addLast("decoder", new MqttMessageDecoder)
    pipeline.addLast("encoder", new MqttMessageEncoder)

    pipeline
  }
}

object MqttServerPipelineFactory extends ChannelPipelineFactory {

  def getPipeline() = {
    val pipeline = Channels.pipeline()

    pipeline.addLast("decoder", new MqttMessageDecoder)
    pipeline.addLast("encoder", new MqttMessageEncoder)

    pipeline
  }
}



