package com.yiguang.mqtt

import java.net.InetSocketAddress

import akka.actor._
import com.twitter.finagle.builder.ServerBuilder
import com.yiguang.mqtt.ClientActor.DispatchInFlightMessage
import com.yiguang.mqtt.MqttServer.ServerManager.StartDispatcher
import com.yiguang.mqtt.provider.{EventBus, RedisEventBus, RedisStorage, Storage}

import scala.collection.concurrent.{Map, TrieMap}
import scala.concurrent.duration._
import com.twitter.util.Duration

/**
 * Created by roger on 3/14/14. 
 */
object MqttServer extends App {

  private[mqtt] object ServerManager {
    def props = Props(classOf[ServerManager])

    private[mqtt] final case class CreateSession(val session: Session)

    private[mqtt] final case class StartDispatcher()

    private[MqttServer] val clients: Map[String, ActorRef] = new TrieMap
  }

  private class ServerManager extends Actor with ActorLogging {

    import com.yiguang.mqtt.MqttModule._
    import com.yiguang.mqtt.MqttServer.ServerManager._

    def receive = {

      case CreateSession(session) =>
        val actor = context.actorOf(ClientActor.props(session), "MqttClient-" + session.clientId)
        context.watch(actor)
        clients.put(session.clientId, actor)
        //start dispatch in flight message
        actor ! DispatchInFlightMessage()

      case StartDispatcher() =>
        val actor = context.actorOf(DispatchActor.props(clients))
        context.watch(actor)
        actor ! "start"

      case m =>
        log.error("Unknown message:" + m)
    }
  }

  private def doMain = {
    import com.yiguang.mqtt.MqttModule._

    val system = ActorSystem("MqttServer")

    //root actor of server
    val serverManager = system.actorOf(ServerManager.props, "ServerManager")

    val mqttService = new MqttService(ServerManager.clients)

    val server = ServerBuilder()
      .codec(MqttCodec(serverManager))
      .bindTo(new InetSocketAddress(Config.serverPort))
      .name("MqttServer")
      .hostConnectionMaxIdleTime(Duration(5,MINUTES))
      .build(mqttService)

    serverManager ! StartDispatcher()
  }

  doMain

}

//DI
private object MqttModule {
  implicit val eventBus: EventBus = RedisEventBus
  implicit val storage: Storage = RedisStorage
}



