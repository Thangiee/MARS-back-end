name := "MARS-back-end"

version := "1.0"

scalaVersion := "2.11.7"

scalacOptions += "-Xexperimental"
// todo: clean up
libraryDependencies ++= {
  val akkaStreamV = "2.0"
  Seq(
    "com.github.t3hnar" % "scala-bcrypt_2.11"                     % "2.5",
    "de.jollyday"       % "jollyday"                              % "0.4.9",
    "net.objectlab.kit" % "datecalc-common"                       % "1.4.0",
    "net.objectlab.kit" % "datecalc-joda"                         % "1.4.0",
    "com.itextpdf"      % "itextpdf"                              % "5.5.8",
    "me.lessis"         %% "courier"                              % "0.1.3",
    "com.github.nscala-time" %% "nscala-time"                     % "2.6.0",
    "com.github.pathikrit" %% "better-files"                      % "2.13.0",
    "org.scalaj"        %% "scalaj-http"                          % "2.1.0",
    "org.postgresql"    % "postgresql"                            % "9.4-1205-jdbc42",
    "com.h2database"    % "h2"                                    % "1.3.166",
    "org.spire-math"    %% "cats"                                 % "0.3.0",
    "com.github.mpilquist" %% "simulacrum"                        % "0.5.0",
    "com.github.cb372"  %% "scalacache-guava"                     % "0.7.3",
    "com.lihaoyi"       %% "ammonite-ops"                         % "0.4.9",
    "ch.qos.logback"    %  "logback-classic"                      % "1.1.3",
    "com.softwaremill.akka-http-session" %% "core"                % "0.2.2",
    "com.typesafe.akka" %% "akka-actor"                           % "2.4.1",
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-experimental"               % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-testkit-experimental"       % akkaStreamV,
    "org.scalatest"     %% "scalatest"                            % "2.2.5"     % "test",
    "org.scalacheck"   %% "scalacheck"                            % "1.12.4"    % "test",
    "org.jvnet.mock-javamail" % "mock-javamail"                   % "1.9"       % "test"
  ).map(_.withJavadoc())
}

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
libraryDependencies +=  "com.typesafe.slick" %% "slick" % "3.1.1"
libraryDependencies +=  "com.typesafe.slick" %% "slick-hikaricp" % "3.1.1"
libraryDependencies += "com.typesafe.akka" % "akka-slf4j_2.11" % "2.4.1"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

fork in run := true

// deployment settings; run "sbt dist"
enablePlugins(JavaServerAppPackaging)
mainClass in Compile := Some("com.utamars.Boot")