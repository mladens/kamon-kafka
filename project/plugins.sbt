lazy val root: Project = project in file(".") dependsOn(RootProject(uri("git://github.com/kamon-io/kamon-sbt-umbrella.git#kamon-2.x")))
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.7")


