package com.amazonaws.services.dynamodbv2

import org.scalatest.{ FunSuite, BeforeAndAfterAll }

import rx.lang.scala.JavaConversions._

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import model._

class AmazonDynamoDBSuite extends FunSuite with BeforeAndAfterAll {

  val creds = new EnvironmentVariableCredentialsProvider
  val db = new AmazonDynamoDBRxNettyClient(creds)

  override def beforeAll() {
  }

  override def afterAll() {
  }

  test("listTables") {
    val r = toScalaObservable(db.listTables()).toBlocking.toList
    assert(r.size == 1)
  }

  test("describeTable") {
    val table = "does_not_exist"
    val req = new DescribeTableRequest().withTableName(table)
    intercept[ResourceNotFoundException] {
      val r = toScalaObservable(db.describeTable(req)).toBlocking.toList
    }
  }
}
