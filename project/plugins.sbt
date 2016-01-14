
libraryDependencies += "com.google.guava" % "guava" % "18.0"

libraryDependencies += "com.github.javaparser" % "javaparser-core" % "2.0.0"

libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.10.27" //withSources()

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.3.3")

addSbtPlugin("com.sksamuel.sbt-versions" % "sbt-versions" % "0.2.0")

//addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "0.94.6")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.5")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.4")
