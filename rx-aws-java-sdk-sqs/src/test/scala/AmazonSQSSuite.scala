/*
 * Copyright 2014-2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.services.sqs

import scala.collection.JavaConversions._

import org.scalatest.{ FunSuite, BeforeAndAfterAll }

import java.util.concurrent.atomic.AtomicReference
import java.net.InetAddress

import rx.lang.scala.JavaConversions._

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import model._

class AmazonSQSSuite extends FunSuite with BeforeAndAfterAll {

  val creds = new EnvironmentVariableCredentialsProvider
  val client = new AmazonSQSRxNettyClient(creds)

  val queueName = s"regression-${System.currentTimeMillis}-${InetAddress.getLocalHost.getHostName}"
  val queueUrl = new AtomicReference[String](null)
  val receiptHandle = new AtomicReference[String](null)

  val testMessage = "test message body"

  override def beforeAll() {
  }

  override def afterAll() {
  }

  ignore("describeQueues - aws") {
    new AmazonSQSClient(creds).listQueues
  }

  ignore("describeQueues") {
    val r = toScalaObservable(client.listQueues).toBlocking.toList
    assert(r.size == 1)
  }

  ignore(s"createQueue - ${queueName}") {
    val r = toScalaObservable(
      client.createQueue(new CreateQueueRequest().withQueueName(queueName))
    ).toBlocking.toList
    assert(r.size == 1)
    queueUrl.set(r.head.result.getQueueUrl)
    assert(Option(queueUrl.get).isDefined)
  }

  ignore(s"sendMessage - ${queueName}") {
    val r = toScalaObservable(
      client.sendMessage(
        new SendMessageRequest().withQueueUrl(queueUrl.get).withMessageBody(testMessage)
      )
    ).toBlocking.toList
    assert(r.size == 1)
  }

  ignore(s"receiveMessage - ${queueName}") {
    val r = toScalaObservable(
      client.receiveMessage(
        new ReceiveMessageRequest().withQueueUrl(queueUrl.get)
      )
    ).toBlocking.toList
    assert(r.size == 1)
    assert(r.head.result.getMessages.size == 1)
    assert(r.head.result.getMessages.head.getBody == testMessage)
    receiptHandle.set(r.head.result.getMessages.head.getReceiptHandle)
  }

  ignore(s"deleteMessage - ${queueName}") {
    val r = toScalaObservable(
      client.deleteMessage(
        new DeleteMessageRequest().withQueueUrl(queueUrl.get).withReceiptHandle(receiptHandle.get)
      )
    ).toBlocking.toList
    assert(r.size == 1)
  }

  ignore(s"deleteQueue - ${queueName}") {
    val r = toScalaObservable(
      client.deleteQueue(new DeleteQueueRequest().withQueueUrl(queueUrl.get))
    ).toBlocking.toList
    assert(r.size == 1)
  }
}
