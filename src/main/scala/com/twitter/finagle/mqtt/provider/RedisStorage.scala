package com.twitter.finagle.mqtt.provider

import com.twitter.finagle.mqtt.protocal.{Topic, PUBLISH}
import redis.clients.jedis.Jedis
import com.twitter.finagle.mqtt.Config

/**
 * Created by roger on 3/26/14. 
 */
object RedisStorage extends Storage{

  private lazy val jedis = new Jedis(Config.redisAddress, Config.redisPort)

  override def getTopicRetainMessages(topicNames: List[String]): List[String] = ???

  override def setTopicRetainMessage(topicName: String, messageId: String): Unit = ???

  override def flushInbox(clientId: String): Unit = ???

  override def getFromInbox(clientId: String): String = ???

  override def addToInbox(clientId: String, messageId: String): Unit = ???

  override def getSubscribedTopics(clientId: String): List[String] = ???

  override def getSubscribers(topicName: String): List[Topic] = ???

  override def unSubscribe(clientId: String): List[String] = ???

  override def unSubscribe(clientId: String, topicNames: List[String]): Unit = ???

  override def subscribe(clientId: String, subscriptions: List[Topic]): Unit = ???

  override def load(id: String): PUBLISH = ???

  override def save(message: PUBLISH): String = ???
}
