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

  test("describeQueues") {
    val r = toScalaObservable(client.listQueues).toBlocking.toList
    assert(r.size == 1)
  }

  test(s"createQueue - ${queueName}") {
    val r = toScalaObservable(
      client.createQueue(new CreateQueueRequest().withQueueName(queueName))
    ).toBlocking.toList
    assert(r.size == 1)
    queueUrl.set(r.head.result.getQueueUrl)
    assert(Option(queueUrl.get).isDefined)
  }

  test(s"sendMessage - ${queueName}") {
    val r = toScalaObservable(
      client.sendMessage(
        new SendMessageRequest().withQueueUrl(queueUrl.get).withMessageBody(testMessage)
      )
    ).toBlocking.toList
    assert(r.size == 1)
  }

  test(s"receiveMessage - ${queueName}") {
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

  test(s"deleteMessage - ${queueName}") {
    val r = toScalaObservable(
      client.deleteMessage(
        new DeleteMessageRequest().withQueueUrl(queueUrl.get).withReceiptHandle(receiptHandle.get)
      )
    ).toBlocking.toList
    assert(r.size == 1)
  }

  test(s"deleteQueue - ${queueName}") {
    val r = toScalaObservable(
      client.deleteQueue(new DeleteQueueRequest().withQueueUrl(queueUrl.get))
    ).toBlocking.toList
    assert(r.size == 1)
  }
}
