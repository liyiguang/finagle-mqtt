package com.twitter.finagle.mqtt.provider


/**
 * Created by roger on 3/21/14. 
 */
trait PubSub {
  def subscribe(clientId: String, topicNames: List[String])

  def unSubscribe(clientId: String, topicNames: List[String])

  def publish(topicName: String, messageId: String)

  def init():PubSub
}





