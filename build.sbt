name := "MARS-back-end"
version := "1.0.0"
scalaVersion := "2.11.8"
scalacOptions ++= Seq("-Xexperimental", "-Ybackend:GenBCode", "-Ydelambdafy:method", "-target:jvm-1.8", "-Yopt:l:classpath")

libraryDependencies += "org.scala-lang.modules" % "scala-java8-compat_2.11" % "0.7.0"

// akka
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-experimental"               % "2.4.4",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % "2.4.4",
  "com.typesafe.akka" %% "akka-http-testkit"                    % "2.4.4",
  "com.typesafe.akka" %% "akka-actor"                           % "2.4.4",
  "com.softwaremill.akka-http-session" %% "core"                % "0.2.3"
).map(_.withJavadoc())

// functional programming libs
libraryDependencies ++= Seq(
  "org.typelevel"        %% "cats"          % "0.5.0",
  "com.github.mpilquist" %% "simulacrum"    % "0.7.0"
)

// database
libraryDependencies ++= Seq(
  "org.postgresql"      % "postgresql"      % "9.4.1207",
  "com.h2database"      % "h2"              % "1.4.190",
  "com.typesafe.slick" %% "slick"           % "3.1.1",
  "com.typesafe.slick" %% "slick-hikaricp"  % "3.1.1"
).map(_.withJavadoc())

// libs to help create time sheet
libraryDependencies ++= Seq(
  "de.jollyday"       % "jollyday"          % "0.4.9",
  "net.objectlab.kit" % "datecalc-common"   % "1.4.0",
  "net.objectlab.kit" % "datecalc-joda"     % "1.4.0",
  "com.itextpdf"      % "itextpdf"          % "5.5.8"
)

// other helper libs
libraryDependencies ++= Seq(
  "com.github.t3hnar"       % "scala-bcrypt_2.11"     % "2.5",      // encryption
  "me.lessis"              %% "courier"               % "0.1.3",    // send email
  "com.github.nscala-time" %% "nscala-time"           % "2.6.0",    // date time
  "com.github.pathikrit"   %% "better-files"          % "2.13.0",   // files
  "org.scalaj"             %% "scalaj-http"           % "2.1.0",    // http request
  "com.github.cb372"       %% "scalacache-guava"      % "0.7.3",    // caching
  "net.virtual-void"       %% "json-lenses"           % "0.6.1",    // json
  "com.sksamuel.scrimage"  %% "scrimage-core"         % "2.1.5"     // image processing
).map(_.withJavadoc())

// logging
libraryDependencies ++= Seq(
  "ch.qos.logback"              %  "logback-classic"  % "1.1.3",
  "com.typesafe.akka"           % "akka-slf4j_2.11"   % "2.4.2",
  "com.typesafe.scala-logging" %% "scala-logging"     % "3.1.0"
)

// testing
libraryDependencies ++= Seq(
  "org.scalatest"          %% "scalatest"       % "2.2.5"     % "test",
  "org.scalacheck"         %% "scalacheck"      % "1.12.4"    % "test",
  "org.jvnet.mock-javamail" % "mock-javamail"   % "1.9"       % "test"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

// deployment settings; run "sbt dist"
enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)

mainClass in Compile := Some("com.utamars.Boot")

// docker settings; run "sbt docker" to generate a docker image and Dockerfile
// https://github.com/marcuslonnberg/sbt-docker
dockerfile in docker := {
  val appDir: File = stage.value
  val targetDir = "/app"
  val port = 8080

  new Dockerfile {
    from("thangiee/mars-base:v1")
    expose(port)

    // environment variables
    // see src/main/resources/application.conf for descriptions
    env("MARS_PRIVATE_ADDR"        , "0.0.0.0")
    env("MARS_PUBLIC_ADDR"         , "")
    env("MARS_PORT"                , s"$port")
    env("MARS_DB_URL"              , "jdbc:postgresql://localhost:5432/postgres")
    env("MARS_DB_USER"             , "")
    env("MARS_DB_PASSWORD"         , "")
    env("MARS_DB_DRIVER"           , "org.postgresql.Driver")
    env("MARS_EMAIL_ADDR"          , "")
    env("MARS_EMAIL_HOST"          , "")
    env("MARS_EMAIL_PORT"          , "587")
    env("MARS_EMAIL_SMTP_USER"     , "")
    env("MARS_EMAIL_SMTP_PASSWORD" , "")
    env("MARS_REG_UUID_TTL_SEC"    , "30")
    env("MARS_FACEPP_KEY"          , "")
    env("MARS_FACEPP_SECRET"       , "")
    env("MARS_TIMESHEET_DIR"       , s"$targetDir/timesheets")
    env("MARS_FACES_DIR"           , s"$targetDir/faces")

    entryPoint(s"$targetDir/bin/${executableScriptName.value}", "-J-server")
    copy(appDir, targetDir)
  }
}

imageNames in docker := Seq(
  ImageName(s"thangiee/${name.value.toLowerCase()}:latest"),
  ImageName(
    namespace = Some("thangiee"),
    repository = name.value.toLowerCase(),
    tag = Some("v" + version.value)
  )
)