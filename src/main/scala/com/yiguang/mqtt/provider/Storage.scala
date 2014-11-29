package com.yiguang.mqtt.provider

import com.yiguang.mqtt.protocal.PUBLISH


/**
 * Created by yigli on 14-11-19.
 */
trait Storage {

  //message
  def saveMessage(message: PUBLISH): String

  def loadMessage(id: String): PUBLISH

  def deleteMessage(id: String)

  //subscribe relationship
  def saveSubscribes(clientId: String, topics: List[(String, Byte)])

  def deleteSubscribes(clientId: String, topicNames: List[String])

  def deleteSubscribes(clientId: String)

  def loadSubscribes(clientId: String): List[(String, Byte)]

  def loadSubscribers(topicName: String): List[(String, Byte)]

  //In flight Message box
  def saveFlightMessage(clientId: String, id: String)

  def deleteFromFlightMessageBox(clientId: String): String

  def deleteFlightMessageBox(clientId: String)

  //retain message
  def updateRetainMessage(topicName: String, id: String)

  def loadRetainMessages(topicNames: List[String]): List[(String, String)]

  //messageId
  def genMessageId(): Int

  //id for storage
  def genId(): String
}
