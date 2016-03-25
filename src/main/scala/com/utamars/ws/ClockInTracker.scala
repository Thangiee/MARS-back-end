package com.utamars.ws

import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.{FlowShape, OverflowStrategy}
import com.utamars.dataaccess.Assistant
import com.utamars.{ExeCtx, util}
import com.utamars.ws.ClockInTracker._
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

object ClockInTracker {
  trait Protocol
  case class Join(id: String, client: ActorRef) extends Protocol
  case class Quit(id: String) extends Protocol
  case class Send(msg: String) extends Protocol
  case object Refresh extends Protocol
  case object NoOp extends Protocol

  case class Finish(data: String)
}

private class AuxActor extends Actor {

  private var clients = Map.empty[String, ActorRef] // track connected clients
  private var cacheData: Option[String] = None
  context.actorOf(Props[ChildActor]) ! "fetch"

  override def receive: Receive = {
    // add new client and send the cached data to only that client
    case Join(id, client) => clients += id -> client; cacheData.foreach(data => client ! Send(data))

    case Quit(id)         => clients -= id

    // only when there are clients, create a child actor to get the current clock in assists
    case Refresh           => if (clients.nonEmpty) context.actorOf(Props[ChildActor]) ! "fetch"

    // notify by child actor that it finish fetching the data. Send the new data to all clients
    case Finish(data)     => cacheData = Some(data); clients.values.foreach(_ ! Send(data))

    case _ =>
  }

}

private class ChildActor extends Actor {

  override def receive: Receive = {
    case "fetch" =>
      Await.result(Assistant.findCurrentClockIn.value, 10.seconds).fold(
        err => self ! PoisonPill,
        data => {
          sender() ! Finish(data.map(a => a.copy(imgId = Some(util.mkFaceImgAssetUrl(a.imgId)))).toJson.compactPrint) // tell parent we finish
          self ! PoisonPill
        }
      )
  }
}