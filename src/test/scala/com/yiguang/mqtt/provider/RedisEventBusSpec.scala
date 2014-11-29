package com.yiguang.mqtt.provider

import akka.actor.{ActorSystem}
import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by yigli on 14-11-20.
 */
class RedisEventBusSpec extends FlatSpec with Matchers {

  "Message sent to Eventbus" should "Received by register listener" in {

    implicit val system = ActorSystem("mqtt")

    val redisEventBus = RedisEventBus

    redisEventBus.registerListener(println _)

    Thread.sleep(1000)

    redisEventBus.fireEvent(PublishEvent("clientId", "topic", "messageId"))

    while (true) {
      Thread.sleep(1000)
    }

  }

}
