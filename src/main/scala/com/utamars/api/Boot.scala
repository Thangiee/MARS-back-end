package com.utamars.api

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import com.utamars.dataaccess._

import scalacache._
import scalacache.guava._

object Boot extends App with LazyLogging {
  val config = ConfigFactory.load()

  implicit val system       = ActorSystem("MARS", config)
  implicit val dispatcher   = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val scalaCache   = ScalaCache(GuavaCache())

  val interface = config.getString("http.interface")
  val port      = config.getInt("http.port")

  val services =
    RegisterUUIDService() ::
    ClockInOutService()   ::
    Nil

  val routes   = pathPrefix("api") { services.map(_.route).reduce(_ ~ _) }
  Http().bindAndHandle(routes, interface, port)

  if (config.getBoolean("db.create")) transaction {
    MySchema.create
    MySchema.printDdl
  }
}

