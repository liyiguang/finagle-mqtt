package com.yiguang.mqtt.provider

import java.util

import com.yiguang.mqtt.Config.Storage
import com.yiguang.mqtt.protocal.{PUBLISH}
import org.slf4s.Logging
import redis.clients.jedis.{JedisPool, Jedis}
import com.yiguang.util.StringUtils._
import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection._
import RedisKey._


/**
 * Created by yigli on 14-11-19.
 */
object RedisStorage extends Storage with Logging {
  private[this] val jedisPool = new JedisPool(Storage.redisAddress, Storage.redisPort)

  private[this] def redis[T](f: Jedis => T): T = {
    val jedis = jedisPool.getResource
    try {
      f(jedis)
    }
    finally
      jedisPool.returnResourceObject(jedis)
  }

  //message
  def saveMessage(message: PUBLISH): String = {
    redis { jedis =>
      val id = genId
      message.id = id
      jedis.set(MKey(id), message.toBytes)

      id
    }
  }

  def loadMessage(id: String): PUBLISH = {
    redis { jedis =>
      val m = jedis.get(toBytes(MKey(id)))
      PUBLISH(m)
    }
  }

  def deleteMessage(id: String) = {
    redis { jedis =>
      jedis.del(MKey(id))
    }
  }

  //subscribe relationship
  def saveSubscribes(clientId: String, topics: List[(String, Byte)]) = {
    redis { jedis =>
      //client -> topics
      val topicList = topics.map { x => (x._1, x._2.toString)}
      val topicMap = mutable.Map[String, String](topicList: _*)
      jedis.hmset(CTKey(clientId), topicMap)

      //topics -> client
      for ((topic, qos) <- topics) {
        jedis.hset(TCKey(topic), clientId, qos.toString)
      }
    }
  }

  def deleteSubscribes(clientId: String, topicNames: List[String]) = {
    redis { jedis =>
      jedis.hdel(CTKey(clientId), topicNames: _*)
    }
  }

  def deleteSubscribes(clientId: String) = {
    redis { jedis =>
      jedis.del(CTKey(clientId))
    }
  }

  def loadSubscribes(clientId: String): List[(String, Byte)] = {
    redis { jedis =>
      val topics: mutable.Map[String, String] = jedis.hgetAll(CTKey(clientId))
      topics.mapValues[Byte](_.toByte).toList
    }
  }

  def loadSubscribers(topicName: String): List[(String, Byte)] = {
    redis { jedis =>
      //FIXME:
      val clients: mutable.Map[String, String] = jedis.hgetAll(TCKey(topicName))
      clients.mapValues[Byte](_.toByte).toList
    }
  }

  //In flight Message box
  def saveFlightMessage(clientId: String, id: String) = {
    redis { jedis =>
      jedis.sadd(IKEY(clientId), id)
    }
  }

  def deleteFromFlightMessageBox(clientId: String): String = {
    redis { jedis =>
      jedis.spop(IKEY(clientId))
    }
  }

  def deleteFlightMessageBox(clientId: String) = {
    redis { jedis =>
      jedis.del(IKEY(clientId))
    }
  }

  //retain message
  def updateRetainMessage(topicName: String, id: String) = {
    redis { jedis =>
      jedis.hset(Storage.keyTopicRemain, topicName, id)
    }
  }

  def loadRetainMessages(topicNames: List[String]): List[(String, String)] = {
    redis { jedis =>
      val temp: mutable.Buffer[String] = jedis.hmget(Storage.keyTopicRemain, topicNames: _*)
      topicNames.zip(temp)

      return null
    }
  }

  //messageId
  def genMessageId(): Int = {
    redis { jedis =>
      jedis.incr(Storage.keyMessageIdGen).toInt
    }
  }

  //id for storage
  def genId(): String = {
    redis { jedis =>
      jedis.incr((Storage.keyMessageStoreIdGen)).toString
    }
  }

}

object RedisKey {

  object CTKey{
    def apply(clientId:String):String = {
      Storage.keyPrefixClientTopics + clientId
    }
  }

  object TCKey {
    def apply(topic:String):String = {
      Storage.keyPrefixTopicClients + topic
    }
  }

  object MKey{
    def apply(id:String):String = {
      Storage.keyPrefixMessage + id
    }
  }

  object IKEY {
    def apply(clientId:String):String = {
      Storage.keyPrefixClientInflight + clientId
    }
  }
}



