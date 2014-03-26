package com.twitter.finagle.mqtt

import com.typesafe.config.ConfigFactory

/**
 * Created by roger on 3/25/14. 
 */
object Config {
  val conf = ConfigFactory.load()

  def serverPort = conf.getInt("server-port")
  def redisAddress = conf.getString("redis.address")
  def redisPort = conf.getInt("redis.port")
  def redisKeyPrefix = conf.getString("redis.key-prefix")
}
