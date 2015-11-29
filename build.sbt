name := "MARS-back-end"

version := "1.0"

scalaVersion := "2.11.7"

scalacOptions += "-Xexperimental"

libraryDependencies ++= {
  val akkaV       = "2.4.0"
  val akkaStreamV = "2.0-M1"
  Seq(
    "org.postgresql"    % "postgresql"                            % "9.4-1205-jdbc42",
    "com.h2database"    % "h2"                                    % "1.3.166",
    "org.squeryl"       %% "squeryl"                              % "0.9.6-RC4",
    "org.spire-math"    %% "cats"                                 % "0.3.0",
    "com.github.mpilquist" %% "simulacrum"                        % "0.5.0",
    "com.github.cb372"  %% "scalacache-guava"                     % "0.7.3",
    "com.lihaoyi"       %% "ammonite-ops"                         % "0.4.9",
    "ch.qos.logback"    %  "logback-classic"                      % "1.1.3",
    "com.typesafe.akka" %% "akka-actor"                           % akkaV,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-experimental"               % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-testkit-experimental"       % akkaStreamV,
    "org.scalatest"     %% "scalatest"                            % "2.2.5"     % "test",
     "org.scalacheck"   %% "scalacheck"                           % "1.12.4"    % "test"
  ).map(_.withJavadoc())
}

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
libraryDependencies += "com.typesafe.akka" % "akka-slf4j_2.11" % "2.4.1"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)

fork in run := true

// deployment settings; run "sbt dist"
enablePlugins(JavaServerAppPackaging)
mainClass in Compile := Some("com.utamars.api.Boot")