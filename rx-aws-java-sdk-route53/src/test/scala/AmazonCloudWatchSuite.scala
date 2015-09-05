package com.amazonaws.services.route53

import org.scalatest.{ FunSuite, BeforeAndAfterAll }

import rx.lang.scala.JavaConversions._

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import model._

class AmazonRoute53Suite extends FunSuite with BeforeAndAfterAll {

  val creds = new EnvironmentVariableCredentialsProvider
  val client = new AmazonRoute53RxNettyClient(creds)

  override def beforeAll() {
  }

  override def afterAll() {
  }

  test("listHostedZones") {
    val r = toScalaObservable(client.listHostedZones).toBlocking.toList
    assert(r.size == 1)
  }

  test("listResourceRecordSets") {
    val r = toScalaObservable(client.listResourceRecordSets).toBlocking.toList
    assert(r.size == 1)
  }
}
