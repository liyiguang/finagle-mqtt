package com.twitter.finagle.mqtt.provider

import redis.clients.jedis.{Jedis, JedisPubSub}
import com.twitter.finagle.mqtt.{ActiveClients, Config}
import scala.actors.Actor
import scala.actors.Actor.actor
import org.slf4s.Logging
import com.twitter.finagle.mqtt.protocal.{QoSLevel, PUBLISH, MQTTHeader}


/**
 * Created by roger on 3/25/14. 
 */

private abstract class Event
private case class Publish(topicName:String,MessageId:String) extends Event
private case class Subscribe(clientId: String, topicNames: List[String]) extends Event
private case class UnSubscribe(clientId: String, topicNames: List[String]) extends Event

object RedisPubSub extends PubSub with Actor with Logging {
  private val storage = RedisStorage

  private lazy val publisherJedis = new Jedis(Config.redisAddress, Config.redisPort)
  private lazy val subscriberJedis = new Jedis(Config.redisAddress, Config.redisPort)

  override def publish(topicName: String, messageId: String): Unit = {
     this ! Publish(topicName,messageId)
  }

  override def unSubscribe(clientId: String, topicNames: List[String]): Unit = {
    this ! UnSubscribe(clientId,topicNames)
  }

  override def subscribe(clientId: String, topicNames: List[String]): Unit = {
    this ! Subscribe(clientId,topicNames)
  }

  override def init = {

    //block this actor
    actor {
      subscriberJedis.subscribe(jedisScriber,redisTopicKey("a"))
    }

    start()
    this
  }

  override def act(): Unit = {
     loop {
       react {
         case Publish(topName,messageId) => publisherJedis.publish(redisTopicKey(topName),messageId)
         case Subscribe(clientId,topicNames) =>
           jedisScriber.subscribe(topicNames.map(redisTopicKey.unapply(_)): _*)
         case UnSubscribe(clientId,topicNames) =>
           jedisScriber.unsubscribe(topicNames.map(redisTopicKey.unapply(_)): _*)
       }
     }
  }

  private object jedisScriber extends JedisPubSub {

    override def onPSubscribe(pattern: String, subscribedChannels: Int): Unit = {}

    override def onPUnsubscribe(pattern: String, subscribedChannels: Int): Unit = {}

    override def onUnsubscribe(channel: String, subscribedChannels: Int): Unit = {}

    override def onSubscribe(channel: String, subscribedChannels: Int): Unit = {}

    override def onPMessage(pattern: String, channel: String, message: String): Unit = {}

    override def onMessage(channel: String, messageId: String): Unit = {
      val topicName = redisTopicKey.unapply(channel)
      log.debug("onMessage topicName=" + topicName + ", messageId=" + messageId)
      dispatch(topicName,messageId)
    }
  }

  private def dispatch(topicName:String,messageId:String) = {
    val message0 = storage.load(messageId)
    for (s <- storage.getSubscribers(topicName)) {
      val header = MQTTHeader.PUBLISH.qosLevel(math.min(s.qos, message0.header.qosLevel).toByte)
      val message = PUBLISH(header, topicName, messageId.toInt, message0.payload)

      ActiveClients.client(s.clienId) match {
        case Some(client) => client ! message
        case None => if(message.header.qosLevel > QoSLevel.AT_MOST_ONCE){
          storage.addToInbox(s.clienId, messageId)
          log.info("inflight message:" + s.clienId + "," + messageId)
        }
      }
    }
  }
}




private object redisTopicKey {
  private lazy val prefix = Config.redisKeyPrefix + ":ptopic:"

  def apply(topicName:String) = prefix + topicName

  def unapply(redisKey:String) = redisKey.substring(prefix.length)

}
