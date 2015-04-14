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
      `rx-aws-java-sdk`
    )
    .settings(buildSettings: _*)
    .settings(BuildSettings.noPackaging: _*)

  lazy val `rx-aws-java-sdk` = project
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsObjectMapper,
      Dependencies.jzlib,
      Dependencies.rxjava,
      Dependencies.rxnettyCore,
      Dependencies.rxnettyCtxts,
      Dependencies.spectatorApi,
      Dependencies.slf4jApi,
      "io.reactivex" %% "rxscala" % "0.24.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value)
      }
    )

  lazy val commonDeps = Seq(
    Dependencies.junitInterface % "test",
    Dependencies.scalatest % "test"
  )

  lazy val checkLicenseHeaders = taskKey[Unit]("Check the license headers for all source files.")
  lazy val formatLicenseHeaders = taskKey[Unit]("Fix the license headers for all source files.")
}
