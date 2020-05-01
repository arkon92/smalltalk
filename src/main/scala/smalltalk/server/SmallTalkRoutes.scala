package smalltalk.server

import cats.effect.{Clock, ContextShift, Sync}
import cats.syntax.semigroupal._
import fs2.Stream._
import fs2.concurrent.{Queue, Topic}
import fs2.{Pipe, Stream}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.{Close, Text}
import smalltalk.domain.{InputMessage, OutputMessage, User}
import smalltalk.server.SmallTalkRoutes.serverUser

import scala.concurrent.duration.SECONDS

class SmallTalkRoutes[F[_]: Sync: ContextShift: Clock](
  queue: Queue[F, InputMessage],
  topic: Topic[F, OutputMessage]
) extends Http4sDsl[F] {

  def routes: HttpRoutes[F] =
    HttpRoutes
      .of[F] {
        case GET -> Root / "smalltalk" / username =>
          val user = User(username)

          val toClient: Stream[F, Text] = topic
            .subscribe(50)
            .map(msg => Text(msg.asJson.noSpaces))

          def processInput(
            wsfStream: Stream[F, WebSocketFrame]
          ): Stream[F, Unit] = {

            val entryStream: Stream[F, InputMessage] =
              Stream
                .eval(
                  Clock[F]
                    .realTime(SECONDS)
                )
                .map { now =>
                  InputMessage(
                    user = serverUser,
                    value = s"Welcome $username!",
                    now
                  )
                }

            val parsedWebSocketInput: Stream[F, InputMessage] =
              wsfStream
                .product(
                  Stream
                    .eval(
                      Clock[F]
                        .realTime(SECONDS)
                    )
                )
                .collect {
                  case (Text(text, _), timestamp) =>
                    InputMessage(user = user, value = text, timestamp)

                  case (Close(_), timestamp) =>
                    InputMessage(serverUser, s"$username left!", timestamp)

                  case (_, timestamp) =>
                    InputMessage(
                      serverUser,
                      "Invalid message received",
                      timestamp
                    )

                }

            (entryStream ++ parsedWebSocketInput).through(queue.enqueue)
          }

          val fromClient: Pipe[F, WebSocketFrame, Unit] = processInput

          WebSocketBuilder[F].build(toClient, fromClient)
      }

}

object SmallTalkRoutes {

  val serverUser: User = User("server")

}
