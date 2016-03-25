package com.utamars.ws

import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.{FlowShape, OverflowStrategy}
import com.utamars.dataaccess.Assistant
import com.utamars.{ExeCtx, util}
import spray.json._
import com.utamars.util.JsonImplicits._

import scala.concurrent.duration._
import scala.concurrent.Await

/**
 * Provide real time update of currently clocked in assistants over web socket.
 */
class ClockInTracker(implicit system: ActorSystem, ec: ExeCtx) {

  private val auxActor = system.actorOf(Props(classOf[AuxActor]))

  def webSocketFlow(): Flow[Message, Message, Any] =
    Flow.fromGraph(GraphDSL.create(Source.actorRef[Send](bufferSize = 5, OverflowStrategy.fail)) { implicit builder =>
      sendData => {  // materialize whenever auxActor emits a Send Protocol

        val fromWebsocket   = builder.add(Flow[Message].map(_ => NoOp)) // ignore all incoming msg from web socket
        val backToWebsocket = builder.add(Flow[Send].map(send => TextMessage(send.msg)))

        val uuid = UUID.randomUUID().toString
        // on new connection, create a Join Protocol holding new client info
        val actorAsSrc = builder.materializedValue.map(actorRef => Join(uuid, actorRef))
        // send any incoming Protocol to the auxActor. Also, send a Quit Protocol if the client leave/disconnect.
        val actorSink = Sink.actorRef[Protocol](auxActor, Quit(uuid))

        val injectKeepAlive = builder.add(Flow[Send].keepAlive(30.second, () => Send("")))

        val merge = builder.add(Merge[Protocol](2))

        fromWebsocket ~> merge.in(0)
        actorAsSrc    ~> merge.in(1)
                         merge ~> actorSink

        sendData ~> injectKeepAlive ~> backToWebsocket

        FlowShape(fromWebsocket.in, backToWebsocket.out)
      }
    })

  def refresh(): Unit = auxActor ! Refresh
}

private trait Protocol
private case class Join(id: String, client: ActorRef) extends Protocol
private case class Quit(id: String) extends Protocol
private case class Send(msg: String) extends Protocol
private case object Refresh extends Protocol
private case object NoOp extends Protocol

private case class Fetch(onComplete: String => Unit)

private class AuxActor extends Actor {
  private var clients = Map.empty[String, ActorRef] // track connected clients

  override def receive: Receive = {
    case Join(id, client)        => clients += id -> client; fetchClockInAsst((data) => client ! Send(data))
    case Quit(id)                => clients -= id
    case Refresh                 => if (clients.nonEmpty) fetchClockInAsst((data) => clients.values.foreach(_ ! Send(data)))
    case _ =>
  }

  def fetchClockInAsst(onComplete: String => Unit): Unit = context.actorOf(Props[ChildActor]) ! Fetch(onComplete)
}

private class ChildActor extends Actor {
  override def receive: Receive = {
    case Fetch(onComplete) =>
      Await.result(Assistant.findCurrentClockIn.value, 10.seconds).fold(
        err => self ! PoisonPill,
        clockInAssts => {
          val json = clockInAssts.map(a => a.copy(imgId = Some(util.mkFaceImgAssetUrl(a.imgId)))).toJson.compactPrint
          onComplete(json)
          self ! PoisonPill
        }
      )
  }
}