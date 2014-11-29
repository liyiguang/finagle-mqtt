package com.yiguang.mqtt.provider

import com.yiguang.mqtt.Config.EventBus
import org.slf4s.Logging
import redis.clients.jedis.{Jedis, JedisPubSub}

/**
 * Created by yigli on 14-11-18.
 */
object RedisEventBus extends EventBus with Logging {

  @volatile
  private var running = false

  private val EVENT_BUS_KEY_PUBLISH = EventBus.key

  private lazy val subscriberJedis = new Jedis(EventBus.redisAddress, EventBus.redisPort)

  private lazy val publishJedis = new Jedis(EventBus.redisAddress, EventBus.redisPort)


  private[this] def init(): Unit = {
    running = true
    val thread = new Thread(new Runnable() {
      override def run(): Unit = {
        while (running) {
          try {
            /**
             * flash disconnected may lost the event
             */
            subscriberJedis.subscribe(jedisScriber, EVENT_BUS_KEY_PUBLISH)
          }
          catch {
            case e =>
              log.error("Subscribe error", e.asInstanceOf[Throwable])
          }
          Thread.sleep(1000)
        }
      }
    })
    thread.setName("RedisEventBus")
    thread.setDaemon(true)
    thread.start()
  }

  override def fireEvent[E <: Event](e: E): Unit = {
    e match {
      case p: PublishEvent =>
        publishJedis.publish(EVENT_BUS_KEY_PUBLISH, p.toJson())
      case _ => log.error("Unsupported Message Type " + e)
    }

  }

  override def registerListener[E <: Event](listener: (E) => Unit) = {
    listener match {
      case l: (PublishEvent => Unit) => jedisScriber.listener = l
      case _ => log.error("Unsupported Listener")
    }
  }

  private[this] object jedisScriber extends JedisPubSub {

    @volatile var listener: PublishEvent => Unit = _

    override def onPSubscribe(pattern: String, subscribedChannels: Int): Unit = {}

    override def onPUnsubscribe(pattern: String, subscribedChannels: Int): Unit = {}

    override def onUnsubscribe(channel: String, subscribedChannels: Int): Unit = {}

    override def onSubscribe(channel: String, subscribedChannels: Int): Unit = {}

    override def onPMessage(pattern: String, channel: String, message: String): Unit = {}

    override def onMessage(channel: String, message: String): Unit = {
      log.debug("Received message:" + message + " on channel:" + channel)
      channel match {
        case EVENT_BUS_KEY_PUBLISH =>
          val p = PublishEvent.fromJson(message)
          p match {
            case Some(m) => listener(m)
            case None => log.error("Not a publish event:" + message)
          }
        case _ =>
          log.error("Unsupported channel:" + channel)
      }
    }
  }

  init()
}


