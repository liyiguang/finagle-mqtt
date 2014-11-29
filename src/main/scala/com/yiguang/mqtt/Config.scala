package com.yiguang.mqtt

import com.typesafe.config.ConfigFactory

/**
 * Created by roger on 3/25/14. 
 */
object Config {

  val conf = ConfigFactory.load()

  val serverPort = conf.getInt("server-port")

  object Storage {
    val redisAddress = conf.getString("storage.redis.address")
    val redisPort = conf.getInt("storage.redis.port")
    val keyPrefixMessage = conf.getString("storage.redis.key-prefix-message")
    val keyPrefixClientTopics = conf.getString("storage.redis.key-prefix-client-topics")
    val keyPrefixTopicClients = conf.getString("storage.redis.key-prefix-topic-clients")
    val keyPrefixClientInflight = conf.getString("storage.redis.key-prefix-inflight-message")

    val keyTopicRemain = conf.getString("storage.redis.key-topic-remain")
    val keyMessageIdGen = conf.getString("storage.redis.key-message-id")
    val keyMessageStoreIdGen = conf.getString("storage.redis.key-message-sid")
  }

  object EventBus {
    val redisAddress = conf.getString("eventbus.redis.address")
    val redisPort = conf.getInt("eventbus.redis.port")
    val key = conf.getString("eventbus.redis.key")
  }

}
