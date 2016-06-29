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

  ignore("describeAlarms") {
    val r = toScalaObservable(client.describeAlarms).toBlocking.toList
    assert(r.size == 1)
  }
}
