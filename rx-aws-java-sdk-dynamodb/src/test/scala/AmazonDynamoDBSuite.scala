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
package com.amazonaws.services.dynamodbv2

import scala.collection.JavaConversions._

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

  ignore("listTables") {
    val r = toScalaObservable(db.listTables()).toBlocking.toList
    assert(r.size == 1)
  }

  ignore("describeTable - error") {
    val table = "does_not_exist"
    val req = new DescribeTableRequest().withTableName(table)
    intercept[ResourceNotFoundException] {
      val r = toScalaObservable(db.describeTable(req)).toBlocking.toList
    }
  }

  ignore("createTable") {
    val tableName = "rx-bpitman-test"
    val keyName = "name"
    val req = new CreateTableRequest()
      .withTableName(tableName)
      .withKeySchema(new KeySchemaElement(keyName, KeyType.HASH))
      .withAttributeDefinitions(new AttributeDefinition(keyName, "S"))
      .withProvisionedThroughput(
        new ProvisionedThroughput()
          .withReadCapacityUnits(1)
          .withWriteCapacityUnits(1)
      )
    val result = toScalaObservable(db.createTable(req)).toBlocking.single
    assert(result.result.getTableDescription.getTableName == tableName)
  }

  ignore("describeTable") {
    val tableName = "rx-bpitman-test"
    val req = new DescribeTableRequest().withTableName(tableName)
    val r = toScalaObservable(db.describeTable(req)).toBlocking.toList
    assert(r.size == 1)
    assert(r(0).result.getTable.getTableName == tableName) 
  }

  ignore("scan") {
    val tableName = "rx-bpitman-test"
    val req = new ScanRequest().withTableName(tableName)
    val r = toScalaObservable(db.scan(req)).toBlocking.toList
    assert(r.size == 1)
    assert(r(0).result.getItems.size == 1)
  }

  case class TestData(k: String, s: String, i: Int, timestamp: Long) {
    def json = {
      s"""{ \"k\": \"${k}\", \"s\": \"${s}\", \"i\": \"${i}\", \"timestamp\": \"${timestamp}\" }"""
    }
  }

  ignore("put") {
    val tableName = "rx-bpitman-test"
    val key = "test"
    val data = TestData(key, "a", 1, 123456789L)
    val e: Option[TestData] = None
    val attributes = Map(
      "name" -> new AttributeValue(key),
      "data" -> new AttributeValue(data.json),
      "timestamp" -> new AttributeValue(data.timestamp.toString)
    )
    val req = new PutItemRequest()
      .withTableName(tableName)
      .withItem(attributes)
      .withExpected(Map( "timestamp" -> {
        e.map(d => new AttributeValue(d.timestamp.toString)) match {
          case None =>  new ExpectedAttributeValue().withExists(false)
          case Some(a) => new ExpectedAttributeValue().withExists(true).withValue(a)
      }}))
    val r = toScalaObservable(db.putItem(req)).toBlocking.toList
    assert(r.size == 1)
  }
}
