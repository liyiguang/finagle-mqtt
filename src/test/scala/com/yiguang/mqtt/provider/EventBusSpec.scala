package com.yiguang.mqtt.provider

import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by yigli on 14-11-19.
 */
class EventBusSpec extends FlatSpec with Matchers {


  "Publish Event" should "serialize and unSerialize" in {
    //    val e = PublishEvent("client","topic","messageId")
    //    val s = e.toBytes()
    //    val u = PublishEvent.fromBytes(s)
    //
    //    assert(u.messageId == e.messageId)
    //    assert(u.topicName == e.topicName)

    val e = PublishEvent("client", "topic", "messageId")
    val json = e.toJson()
    val p2 = PublishEvent.fromJson(json).getOrElse(PublishEvent("", "", ""))

    assert(p2.messageId == e.messageId)
    assert(p2.topicName == e.topicName)

  }
}
