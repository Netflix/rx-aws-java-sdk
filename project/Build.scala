import sbt._
import sbt.Keys._
import com.typesafe.sbt.pgp.PgpKeys._

object MainBuild extends Build {

  lazy val baseSettings =
    sbtrelease.ReleasePlugin.releaseSettings ++
    Sonatype.settings ++
    net.virtualvoid.sbt.graph.Plugin.graphSettings ++
    scoverage.ScoverageSbtPlugin.projectSettings

  lazy val buildSettings = baseSettings ++ Seq(
            organization := BuildSettings.organization,
            scalaVersion := Dependencies.Versions.scala,
              crossPaths := false,
           sourcesInBase := false,
        autoScalaLibrary := false,
       externalResolvers := BuildSettings.resolvers,
     checkLicenseHeaders := License.checkLicenseHeaders(streams.value.log, sourceDirectory.value),
    formatLicenseHeaders := License.formatLicenseHeaders(streams.value.log, sourceDirectory.value)
  )

  lazy val root = project.in(file("."))
    .aggregate(
      `rx-aws-java-sdk-core`,
      `rx-aws-java-sdk-dynamodb`,
      `rx-aws-java-sdk-ec2`
    )
    .settings(buildSettings: _*)
    .settings(BuildSettings.noPackaging: _*)

  lazy val `rx-aws-java-sdk-core` = project
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk-core" % "1.9.16",
      Dependencies.jzlib,
      Dependencies.rxjava,
      Dependencies.rxnettyCore,
      Dependencies.rxnettyCtxts,
      Dependencies.spectatorApi,
      Dependencies.slf4jApi,
      "io.reactivex" %% "rxscala" % "0.24.0" % "test"
    ))

  lazy val `rx-aws-java-sdk-dynamodb` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.9.16",
      "io.reactivex" %% "rxscala" % "0.24.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("dynamodb", "dynamodbv2"))
      }
    )

  lazy val `rx-aws-java-sdk-ec2` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk-ec2" % "1.9.16",
      "io.reactivex" %% "rxscala" % "0.24.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("ec2"))
      }
    )

  lazy val commonDeps = Seq(
    Dependencies.junitInterface % "test",
    Dependencies.scalatest % "test"
  )

  lazy val checkLicenseHeaders = taskKey[Unit]("Check the license headers for all source files.")
  lazy val formatLicenseHeaders = taskKey[Unit]("Fix the license headers for all source files.")
}
