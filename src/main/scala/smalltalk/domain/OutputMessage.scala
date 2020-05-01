package smalltalk.domain

import java.time.{Instant, ZoneId}
import java.time.format.{DateTimeFormatter, FormatStyle}
import java.util.Locale

final case class OutputMessage(user: User, value: String, date: String)

object OutputMessage {

  private val formatter =
    DateTimeFormatter
      .ofLocalizedDateTime(FormatStyle.SHORT)
      .withLocale(Locale.FRANCE)
      .withZone(ZoneId.systemDefault());

  def fromInputMessage(inputMessage: InputMessage): OutputMessage = {
    OutputMessage(
      user = inputMessage.user,
      value = inputMessage.value,
      date = formatter.format(Instant.ofEpochSecond(inputMessage.timestamp))
    )
  }

}
