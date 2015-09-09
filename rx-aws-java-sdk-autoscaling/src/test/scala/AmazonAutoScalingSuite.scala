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
