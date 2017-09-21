package com.github.rymurphy12.analytics

import com.danielasfregola.twitter4s.TwitterStreamingClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken, Tweet}
import com.danielasfregola.twitter4s.entities.streaming.StreamingMessage

object Analyzer{
  def main(args: Array[String]): Unit = {
    val consumerToken = ConsumerToken("place holder",
                                      "place holder" )
    val accessToken = AccessToken("place holder",
                                  "place holder")

    val streamingTwitterClient = TwitterStreamingClient(consumerToken, accessToken)
    streamingTwitterClient.sampleStatuses(Seq())(printStream)

  }

  def printStream: PartialFunction[StreamingMessage, Unit] = {
    case x: Tweet => println(x)
  }
}
