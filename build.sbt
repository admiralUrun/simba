name := "simba"

version := "1.0"

lazy val `simba` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(jdbc, ws, specs2 % Test, guice,
  "org.typelevel" %% "cats-core" % "2.1.1",
  "org.tpolecat" %% "doobie-core" % "0.8.8",
  "org.tpolecat" %% "doobie-h2" % "0.8.8",
  "mysql" % "mysql-connector-java" % "5.1.12",
  "org.webjars" % "bootstrap" % "4.4.1"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

