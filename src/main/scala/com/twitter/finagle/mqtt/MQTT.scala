package com.twitter.finagle.mqtt

import com.twitter.finagle.stats.{Gauge, Stat, Counter, StatsReceiver}
import com.twitter.finagle.{Service, Codec, CodecFactory}
import com.twitter.finagle.mqtt.protocal._
import com.twitter.util.{Closable, JavaSingleton}
import org.jboss.netty.channel.{Channels, ChannelPipelineFactory}
import com.twitter.finagle.transport.Transport

/**
 * Created by roger on 3/13/14. 
 */

object MQTT {
  def apply() = new MQTT()
}


class MQTT() extends CodecFactory[MQTTMessage,MQTTMessage]{

  def server = Function.const {
    new Codec[MQTTMessage, MQTTMessage] {
      def pipelineFactory = MQTTServerPipelineFactory

      override def newServerDispatcher(transport: Transport[Any, Any], service: Service[MQTTMessage, MQTTMessage]): Closable = {
        new MQTTServerDispacher(transport.cast[MQTTMessage,MQTTMessage],service)
      }
    }
  }

  def client = Function.const {
    new Codec[MQTTMessage, MQTTMessage] {
      def pipelineFactory = MQTTClientPipelineFactory
    }
  }
}

object MQTTClientPipelineFactory extends ChannelPipelineFactory {
  def getPipeline() = {
    val pipeline = Channels.pipeline()

    pipeline.addLast("decoder", new MQTTMessageDecoder)
    pipeline.addLast("encoder", new MQTTMessageEncoder)

    pipeline
  }
}

object MQTTServerPipelineFactory extends ChannelPipelineFactory {

  def getPipeline() = {
    val pipeline = Channels.pipeline()

    pipeline.addLast("decoder", new MQTTMessageDecoder)
    pipeline.addLast("encoder", new MQTTMessageEncoder)

    pipeline
  }
}



