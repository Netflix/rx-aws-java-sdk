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

  ignore("listHostedZones") {
    val r = toScalaObservable(client.listHostedZones).toBlocking.toList
    assert(r.size == 1)
  }

  ignore("listResourceRecordSets") {
    val r = toScalaObservable(client.listResourceRecordSets).toBlocking.toList
    assert(r.size == 1)
  }
}
