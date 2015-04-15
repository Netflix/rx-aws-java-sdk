package com.amazonaws.services.ec2

import org.scalatest.{ FunSuite, BeforeAndAfterAll }

import rx.lang.scala.JavaConversions._

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import model._

class AmazonEC2Suite extends FunSuite with BeforeAndAfterAll {

  val creds = new EnvironmentVariableCredentialsProvider
  val ec2 = new AmazonEC2RxNettyClient(creds)

  override def beforeAll() {
  }

  override def afterAll() {
  }

  test("describeVpc") {
    val r = toScalaObservable(ec2.describeVpcs).toBlocking.toList
    assert(r.size == 1)
  }
}
