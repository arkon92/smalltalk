package smalltalk

import cats.effect._
import fs2.Stream
import fs2.concurrent.{Queue, Topic}
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.syntax.kleisli._
import smalltalk.domain.{InputMessage, OutputMessage}
import smalltalk.server.SmallTalkRoutes
import smalltalk.server.SmallTalkRoutes.serverUser

import scala.concurrent.duration._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    for {
      queue <- Queue.bounded[IO, InputMessage](100)
      topic <- Topic[IO, OutputMessage](
        OutputMessage(serverUser, "KeepAlive", "")
      )

      exitCode <- {
        val httpStream = ServerStream.stream[IO](5020, queue, topic)

        val keepAlive = Stream
          .awakeEvery[IO](30.seconds)
          .map(_ => OutputMessage(serverUser, "KeepAlive", ""))
          .through(topic.publish)

        val processingStream =
          queue.dequeue
            .map(msg => OutputMessage.fromInputMessage(msg))
            .flatMap(Stream.emit)
            .through(topic.publish)

        Stream(httpStream, keepAlive, processingStream).parJoinUnbounded.compile.drain
          .as(ExitCode.Success)
      }
    } yield exitCode

}

object ServerStream {
  def stream[F[_]: ConcurrentEffect: Timer: ContextShift](
    port: Int,
    queue: Queue[F, InputMessage],
    topic: Topic[F, OutputMessage],
  ): fs2.Stream[F, ExitCode] =
    BlazeServerBuilder[F]
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(
        Router("/" -> new SmallTalkRoutes[F](queue, topic).routes).orNotFound
      )
      .serve
}
