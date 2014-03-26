package com.twitter.finagle.mqtt.provider

import com.twitter.finagle.mqtt.protocal.{Topic, PUBLISH}

/**
 * Created by roger on 3/21/14. 
 */
trait Storage {
  def save(message: PUBLISH): String

  def load(id: String): PUBLISH

  def subscribe(clientId: String, subscriptions: List[Topic])

  def unSubscribe(clientId: String, topicNames: List[String])

  def unSubscribe(clientId: String): List[String]

  def getSubscribers(topicName: String): List[Topic]

  def getSubscribedTopics(clientId: String): List[String]

  def addToInbox(clientId: String, messageId: String)

  def getFromInbox(clientId: String): String

  def flushInbox(clientId: String)

  def setTopicRetainMessage(topicName: String, messageId: String)

  def getTopicRetainMessages(topicNames: List[String]): List[String]
}
