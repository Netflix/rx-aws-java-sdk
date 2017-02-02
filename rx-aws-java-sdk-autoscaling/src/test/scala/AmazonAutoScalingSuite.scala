/*
 * Copyright 2014-2017 Netflix, Inc.
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
package com.amazonaws.services.autoscaling

import org.scalatest.{ FunSuite, BeforeAndAfterAll }

import java.net.InetAddress

import org.apache.commons.codec.binary.Base64

import rx.lang.scala.JavaConversions._

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import model._

class AmazonAutoScalingSuite extends FunSuite with BeforeAndAfterAll {

  val creds = new EnvironmentVariableCredentialsProvider
  val client = new AmazonAutoScalingRxNettyClient(creds)

  override def beforeAll() {
  }

  override def afterAll() {
  }

  val lcName = s"regression-${System.currentTimeMillis}-${InetAddress.getLocalHost.getHostName}"

  ignore("createLaunchConfiguration") {
    val r = toScalaObservable(client.createLaunchConfiguration(
      new CreateLaunchConfigurationRequest()
        .withLaunchConfigurationName(lcName)
        .withImageId("ami-19fa4172")
        .withInstanceType("c3.large")
        .withUserData(new String(Base64.encodeBase64("test".getBytes)))
    )).toBlocking.toList
    assert(r.size == 1)
  }
}
