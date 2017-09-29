package com.github.rymurphy12.analytics

import scala.collection.mutable.ListBuffer

import com.danielasfregola.twitter4s.TwitterStreamingClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken, HashTag, Tweet, UrlDetails}
import com.danielasfregola.twitter4s.entities.streaming.StreamingMessage
import com.typesafe.config.ConfigFactory



object Analyzer{
  type Emoji = String
  val twitterMetrics: ListBuffer[(Option[Seq[UrlDetails]], Option[Seq[HashTag]], Option[List[Emoji]])] = new ListBuffer()

  def main(args: Array[String]): Unit = {
    val consumerToken = ConsumerToken(ConfigFactory.load().getString("twitter.consumer.key"),
                                      ConfigFactory.load().getString("twitter.consumer.secret"))
    val accessToken = AccessToken(ConfigFactory.load().getString("twitter.access.key"),
                                  ConfigFactory.load().getString("twitter.access.secret"))

    val streamingTwitterClient = TwitterStreamingClient(consumerToken, accessToken)
    streamingTwitterClient.sampleStatuses()(collectRawTwitterData)
  }

  def collectRawTwitterData: PartialFunction[StreamingMessage, Unit] = {
    case tweet: Tweet =>  twitterMetrics += pullIndividualTweetData(tweet)
  }

  def pullIndividualTweetData(tweet: Tweet): (Option[Seq[UrlDetails]], Option[Seq[HashTag]], Option[List[Emoji]]) = tweet.entities match {
      case None => (None, None, getEmojis(tweet.text))
      case Some(e) => (Some(e.urls), Some(e.hashtags), getEmojis(tweet.text))
  }

  def getEmojis(tweetText: String): Some[List[Emoji]] = ???


}
