import sbt._
import sbt.Keys._

object BuildSettings {
  val organization = "com.netflix.rx-aws-java-sdk"

  val resolvers = Seq(
    Resolver.mavenLocal,
    Resolver.jcenterRepo,
    DefaultMavenRepository,
    "jfrog" at "http://oss.jfrog.org/oss-snapshot-local",
    "rrd4j" at "https://raw.githubusercontent.com/brharrington/rrd4j/repo")

  // Don't create root.jar, from:
  // http://stackoverflow.com/questions/20747296/producing-no-artifact-for-root-project-with-package-under-multi-project-build-in
  lazy val noPackaging = Seq(
    Keys.`package` :=  file(""),
    packageBin in Global :=  file(""),
    packagedArtifacts :=  Map()
  )
}
