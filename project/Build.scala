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
      `rx-aws-java-sdk-autoscaling`,
      `rx-aws-java-sdk-cloudformation`,
      `rx-aws-java-sdk-cloudfront`,
      `rx-aws-java-sdk-cloudsearch`,
//      `rx-aws-java-sdk-cloudsearchdomain`,
      `rx-aws-java-sdk-cloudwatch`,
      `rx-aws-java-sdk-codedeploy`,
      `rx-aws-java-sdk-cognitoidentity`,
      `rx-aws-java-sdk-cognitosync`,
      `rx-aws-java-sdk-config`,
      `rx-aws-java-sdk-directconnect`,
      `rx-aws-java-sdk-dynamodb`,
      `rx-aws-java-sdk-ec2`,
      `rx-aws-java-sdk-ecs`,
      `rx-aws-java-sdk-elasticache`,
      `rx-aws-java-sdk-elasticloadbalancing`,
      `rx-aws-java-sdk-emr`,
      `rx-aws-java-sdk-elastictranscoder`,
      `rx-aws-java-sdk-glacier`,
      `rx-aws-java-sdk-iam`,
      `rx-aws-java-sdk-importexport`,
      `rx-aws-java-sdk-kinesis`,
      `rx-aws-java-sdk-rds`,
      `rx-aws-java-sdk-redshift`,
      `rx-aws-java-sdk-route53`,
//      `rx-aws-java-sdk-route53domains`,
      //`rx-aws-java-sdk-s3`,
      //`rx-aws-java-sdk-s3encryption`,
      //`rx-aws-java-sdk-simpledb`,
      `rx-aws-java-sdk-ses`,
      `rx-aws-java-sdk-simpleworkflow`,
      `rx-aws-java-sdk-sns`,
      `rx-aws-java-sdk-sqs`,
      `rx-aws-java-sdk-sts`,
      `rx-aws-java-sdk`
    )
    .settings(buildSettings: _*)
    .settings(BuildSettings.noPackaging: _*)

  lazy val `rx-aws-java-sdk-core` = project
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsCore,
      Dependencies.guava,
      Dependencies.jzlib,
      Dependencies.rxjava,
      Dependencies.rxnettyCore,
      Dependencies.rxnettyCtxts,
      Dependencies.spectatorApi,
      Dependencies.slf4jApi,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))

  lazy val `rx-aws-java-sdk-autoscaling` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsAutoScaling,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("autoscaling"))
      }
    )

  lazy val `rx-aws-java-sdk-cloudformation` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsCloudFormation,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("cloudformation"))
      }
    )

  lazy val `rx-aws-java-sdk-cloudfront` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsCloudFront,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("cloudfront"))
      }
    )

  lazy val `rx-aws-java-sdk-cloudsearch` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsCloudSearch,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("cloudsearch", "cloudsearchdomain"))
      }
    )

/**
  lazy val `rx-aws-java-sdk-cloudsearchdomain` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsCloudSearchDomain,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("cloudsearchdomain"))
      }
    )
*/

  lazy val `rx-aws-java-sdk-cloudwatch` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsCloudWatch,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("cloudwatch"))
      }
    )

  lazy val `rx-aws-java-sdk-codedeploy` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsCodeDeploy,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("codedeploy"))
      }
    )

  lazy val `rx-aws-java-sdk-cognitoidentity` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsCognitoIdentity,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("cognitoidentity"))
      }
    )

  lazy val `rx-aws-java-sdk-cognitosync` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsCognitoSync,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("cognitosync"))
      }
    )

  lazy val `rx-aws-java-sdk-config` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsConfig,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("config"))
      }
    )

  lazy val `rx-aws-java-sdk-directconnect` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsDirectConnect,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("directconnect"))
      }
    )

  lazy val `rx-aws-java-sdk-dynamodb` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsDynamoDB,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
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
      Dependencies.awsEC2,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("ec2"))
      }
    )

  lazy val `rx-aws-java-sdk-ecs` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsECS,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("ecs"))
      }
    )

  lazy val `rx-aws-java-sdk-elasticache` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsElasticache,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("elasticache"))
      }
    )

  lazy val `rx-aws-java-sdk-elasticloadbalancing` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsElasticLoadBalancing,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("elasticloadbalancing"))
      }
    )

  lazy val `rx-aws-java-sdk-emr` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsElasticMapReduce,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("elasticmapreduce"))
      }
    )

  lazy val `rx-aws-java-sdk-elastictranscoder` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsElasticTranscoder,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("elastictranscoder"))
      }
    )

  lazy val `rx-aws-java-sdk-glacier` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsGlacier,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("glacier"))
      }
    )

  lazy val `rx-aws-java-sdk-iam` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsIdentityManagement,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("identitymanagement"))
      }
    )

  lazy val `rx-aws-java-sdk-importexport` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsImportExport,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("importexport"))
      }
    )

  lazy val `rx-aws-java-sdk-kinesis` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsKinesis,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("kinesis"))
      }
    )

  lazy val `rx-aws-java-sdk-rds` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsRDS,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("rds"))
      }
    )

  lazy val `rx-aws-java-sdk-redshift` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsRedShift,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("redshift"))
      }
    )

  lazy val `rx-aws-java-sdk-route53` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsRoute53,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("route53", "route53domains"))
      }
    )

/**
  lazy val `rx-aws-java-sdk-route53domains` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsRoute53Domains,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("route53domains"))
      }
    )
*/

  lazy val `rx-aws-java-sdk-s3` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsS3,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("s3"))
      }
    )

  lazy val `rx-aws-java-sdk-s3encryption` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsS3Encryption,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("s3encryption"))
      }
    )

  lazy val `rx-aws-java-sdk-simpledb` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsSimpleDB,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("simpledb"))
      }
    )

  lazy val `rx-aws-java-sdk-ses` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsSimpleEmailService,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("simpleemail"))
      }
    )

  lazy val `rx-aws-java-sdk-simpleworkflow` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsSimpleWorkflow,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("simpleworkflow"))
      }
    )

  lazy val `rx-aws-java-sdk-sns` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsSNS,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("sns"))
      }
    )

  lazy val `rx-aws-java-sdk-sqs` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsSQS,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("sqs"))
      }
    )

  lazy val `rx-aws-java-sdk-sts` = project
    .dependsOn(`rx-aws-java-sdk-core`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.awsSTS,
      "io.reactivex" %% "rxscala" % "0.25.0" % "test"
    ))
    .settings(
      sourceGenerators in Compile <+= Def.task {
        AwsGenerate.generate((sourceManaged in Compile).value, List("sts"))
      }
    )

  lazy val `rx-aws-java-sdk` = project
    .dependsOn(`rx-aws-java-sdk-autoscaling`)
    .dependsOn(`rx-aws-java-sdk-cloudformation`)
    .dependsOn(`rx-aws-java-sdk-cloudfront`)
    .dependsOn(`rx-aws-java-sdk-cloudsearch`)
//    .dependsOn(`rx-aws-java-sdk-cloudsearchdomain`)
    .dependsOn(`rx-aws-java-sdk-cloudwatch`)
    .dependsOn(`rx-aws-java-sdk-codedeploy`)
    .dependsOn(`rx-aws-java-sdk-cognitoidentity`)
    .dependsOn(`rx-aws-java-sdk-cognitosync`)
    .dependsOn(`rx-aws-java-sdk-config`)
    .dependsOn(`rx-aws-java-sdk-directconnect`)
    .dependsOn(`rx-aws-java-sdk-dynamodb`)
    .dependsOn(`rx-aws-java-sdk-ec2`)
    .dependsOn(`rx-aws-java-sdk-ecs`)
    .dependsOn(`rx-aws-java-sdk-elasticache`)
    .dependsOn(`rx-aws-java-sdk-elasticloadbalancing`)
    .dependsOn(`rx-aws-java-sdk-emr`)
    .dependsOn(`rx-aws-java-sdk-elastictranscoder`)
    .dependsOn(`rx-aws-java-sdk-glacier`)
    .dependsOn(`rx-aws-java-sdk-iam`)
    .dependsOn(`rx-aws-java-sdk-importexport`)
    .dependsOn(`rx-aws-java-sdk-kinesis`)
    .dependsOn(`rx-aws-java-sdk-rds`)
    .dependsOn(`rx-aws-java-sdk-redshift`)
    .dependsOn(`rx-aws-java-sdk-route53`)
//    .dependsOn(`rx-aws-java-sdk-route53domains`)
    //.dependsOn(`rx-aws-java-sdk-s3`)
    //.dependsOn(`rx-aws-java-sdk-s3encryption`)
    //.dependsOn(`rx-aws-java-sdk-simpledb`)
    .dependsOn(`rx-aws-java-sdk-ses`)
    .dependsOn(`rx-aws-java-sdk-simpleworkflow`)
    .dependsOn(`rx-aws-java-sdk-sns`)
    .dependsOn(`rx-aws-java-sdk-sqs`)
    .dependsOn(`rx-aws-java-sdk-sts`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)

  lazy val commonDeps = Seq(
    Dependencies.junitInterface % "test",
    Dependencies.scalatest % "test",
    Dependencies.slf4jBinding % "test"
  )

  lazy val checkLicenseHeaders = taskKey[Unit]("Check the license headers for all source files.")
  lazy val formatLicenseHeaders = taskKey[Unit]("Fix the license headers for all source files.")
}
