resolvers += Resolver.url("sbt-plugins", new URL("https://bintray.com/artifact/download/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

libraryDependencies += "com.google.guava" % "guava" % "18.0"

libraryDependencies += "com.github.javaparser" % "javaparser-core" % "2.0.0"

libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.10.5.1" //withSources()

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.0.1")

addSbtPlugin("com.sksamuel.sbt-versions" % "sbt-versions" % "0.2.0")

//addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "0.94.6")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

//addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.1.6")
