package com.amazonaws.services.cloudwatch

import org.scalatest.{ FunSuite, BeforeAndAfterAll }

import rx.lang.scala.JavaConversions._

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import model._

class AmazonCloudWatchSuite extends FunSuite with BeforeAndAfterAll {

  val creds = new EnvironmentVariableCredentialsProvider
  val client = new AmazonCloudWatchRxNettyClient(creds)

  override def beforeAll() {
  }

  override def afterAll() {
  }

  test("describeAlarms") {
    val r = toScalaObservable(client.describeAlarms).toBlocking.toList
    assert(r.size == 1)
  }
}
