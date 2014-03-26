package com.twitter.finagle.mqtt

import com.twitter.finagle.builder.{ServerBuilder, Server}
import java.net.InetSocketAddress
import com.twitter.finagle.mqtt.provider.{RedisStorage, RedisPubSub, Storage, PubSub}

/**
 * Created by roger on 3/14/14. 
 */
object MQTTServer extends App {
  val mqttService = new MQTTService

  val server: Server = ServerBuilder()
    .codec(MQTT())
    .bindTo(new InetSocketAddress(Config.serverPort))
    .name("MQTTServer")
    .build(mqttService)
}

object MQTTModule {
  implicit val pubsub:PubSub = RedisPubSub.init
  implicit val storage:Storage = RedisStorage
}



