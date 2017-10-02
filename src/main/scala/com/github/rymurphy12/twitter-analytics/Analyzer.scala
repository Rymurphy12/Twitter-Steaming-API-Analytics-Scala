package com.github.rymurphy12.analytics

import com.danielasfregola.twitter4s.TwitterRestClient
import org.joda.time.DateTime
import scala.collection.mutable.ListBuffer

import com.danielasfregola.twitter4s.TwitterStreamingClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken, HashTag, Tweet, UrlDetails}
import com.danielasfregola.twitter4s.entities.streaming.StreamingMessage
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import scala.io.Source
import com.vdurmont.emoji._
import scala.collection.JavaConverters._
import akka.actor.ActorSystem
import scala.concurrent.duration._

object Analyzer{
  type Emoji = String
  val twitterMetrics: ListBuffer[TweetMetrics] = new ListBuffer()
  val startTimeInMillis = DateTime.now().getMillis


  def main(args: Array[String]): Unit = {
    val consumerToken = ConsumerToken(ConfigFactory.load().getString("twitter.consumer.key"),
                                      ConfigFactory.load().getString("twitter.consumer.secret"))
    val accessToken = AccessToken(ConfigFactory.load().getString("twitter.access.key"),
                                  ConfigFactory.load().getString("twitter.access.secret"))
     
    val streamingTwitterClient = TwitterStreamingClient(consumerToken, accessToken)
    streamingTwitterClient.sampleStatuses(stall_warnings = true)(collectRawTwitterData)

    val schedulingSystem = ActorSystem("SchedulingForMetrics")
    import schedulingSystem.dispatcher
    schedulingSystem.scheduler.schedule(10 seconds, 15 seconds)(displayCurrentTweetMetrics(twitterMetrics.toList))
  }

  def collectRawTwitterData: PartialFunction[StreamingMessage, Unit] = {
    case tweet: Tweet =>  twitterMetrics += pullIndividualTweetData(tweet)
  }

  def pullIndividualTweetData(tweet: Tweet): TweetMetrics = tweet.entities match {
      case None => TweetMetrics(None, None, getEmojis(tweet.text))
      case Some(e) => TweetMetrics(Some(e.urls), Some(e.hashtags), getEmojis(tweet.text))
  }

  def getEmojis(tweetText: String): Option[Seq[Emoji]] = {
    val extractEmos = EmojiParser.extractEmojis(tweetText).asScala.toSeq
    if (extractEmos.length > 0) Some(extractEmos) else None
  }

  def displayCurrentTweetMetrics(totalTweetMetrics: List[TweetMetrics]): Unit = {
    val currentTimeInMillis = DateTime.now().getMillis

    val totalTweets = totalTweetMetrics.length
    val tweetsPerSecond = (totalTweets / ((currentTimeInMillis - startTimeInMillis)/ 1000.0))
    val tweetsPerMinute = if (currentTimeInMillis - startTimeInMillis < 60000) tweetsPerSecond * 60
                          else totalTweets  / ((currentTimeInMillis - startTimeInMillis ) / 60000.0)
    val tweetsPerHour = if (currentTimeInMillis - startTimeInMillis < 3600000) tweetsPerMinute * 60
                        else totalTweets / ((currentTimeInMillis - startTimeInMillis ) / 3600000.0)

    val topFiveEmojis = getTopFiveEmojis(totalTweetMetrics)
    val emojiPercentage =  (totalTweetMetrics.map(_.emojisInTweet.getOrElse(Seq.empty))
                                             .filter(_.length > 0).length) / totalTweets.toDouble

    val topFiveHashTags = getTopFiveHashTags(totalTweetMetrics)
    val hashTagPercentage = (totalTweetMetrics.map(_.hashTagsInTweet.getOrElse(Seq.empty))
                                              .filter(_.length > 0).length) / totalTweets.toDouble

    val topFiveDomains = getTopFiveDomains(totalTweetMetrics)
    val urlPercentage = (totalTweetMetrics.map(_.urlsInTweet.getOrElse(Seq.empty))
      .filter(_.length > 0).length) / totalTweets.toDouble
    val pictureUrl = getTotalImageUrls(totalTweetMetrics) / totalTweets.toDouble

    println(s"Total Number of Tweets Collected:               $totalTweets")
    println(f"Average Tweets Per Second:                      $tweetsPerSecond%2f")
    println(f"Average Tweets Per Minute:                      $tweetsPerMinute%2f")
    println(f"Average Tweets Per Hour:                        $tweetsPerHour%2f")
    println(f"Percent of Tweets That Contain Emojis:          ${(emojiPercentage * 100)}%2f percent of tweets")
    println(f"Percentage of Tweets That Contain HashTags:     ${(hashTagPercentage * 100)}%2f percent of tweets")
    println(f"Percentage of Tweets That Contain a URL:        ${(urlPercentage * 100)}%2f precent of tweets")
    println(f"Percentage of Tweets That Contin a Picture URL: ${(pictureUrl * 100)}%2f percent of tweets")
    println()
    println("Top Five Emojis:")
    println(topFiveEmojis.mkString("\n"))
    println()
    println("Top Five HashTags:")
    println(topFiveHashTags.mkString("\n"))
    println()
    println("Top Domains:")
    println(topFiveDomains.mkString("\n"))
    println()
  }

  def getTopFiveEmojis(twitterMetricsForEmojis: List[TweetMetrics]): Seq[String] = {
    val allCollectedEmojis = twitterMetricsForEmojis.flatMap(_.emojisInTweet.getOrElse(Seq.empty))

    val emojiFrequencey = allCollectedEmojis.groupBy(identity).mapValues(_.length)
    val topFive = emojiFrequencey.toSeq.sortBy { case (emoji, frequency)  => -frequency }.take(5).zipWithIndex
    topFive.map{case ((emoji, frequency), rank) => s"[${rank + 1}] $emoji (found $frequency times) " }
  }

  def getTopFiveHashTags(twitterMetricsForHashTags: List[TweetMetrics]): Seq[String] = {
    val allCollectedHashTags = twitterMetricsForHashTags.flatMap(_.hashTagsInTweet.getOrElse(Seq.empty))

    val hashTagText = allCollectedHashTags.map(_.text)
    val hashTagFrequency = hashTagText.groupBy(identity).mapValues(_.length)
    val topFive = hashTagFrequency.toSeq.sortBy { case (hashtag, frequency) => -frequency }.take(5).zipWithIndex
    topFive.map{case ((hashtag, frequency), rank) => s"[${rank + 1}] $hashtag (found $frequency times)"}
  }
  
  def getTopFiveDomains(twitterMetricsForDomains: List[TweetMetrics]): Seq[String] = {
    val allCollectedUrls = twitterMetricsForDomains.flatMap(_.urlsInTweet.getOrElse(Seq.empty))

    val urlSimplified = allCollectedUrls.map(_.display_url.split("/").head)
    val urlFrequency = urlSimplified.groupBy(identity).mapValues(_.length)
    val topFive = urlFrequency.toSeq.sortBy { case (url, frequency) => -frequency }.take(5).zipWithIndex
    topFive.map{case ((url, frequency), rank) => s"[${rank + 1}] $url (found $frequency times)"}
  }

  def getTotalImageUrls(twitterMetricsForPicUrls: List[TweetMetrics]): Int = {
    val listOfImageUrls = List("pic.twitter.com", "instagram.com", "flickr.com")
    val totalURls = twitterMetricsForPicUrls.map(_.urlsInTweet.getOrElse(Seq.empty))
    totalURls.map(seq => seq.map(urlDetail => urlDetail.display_url.split("/").head)
             .filter(listOfImageUrls.contains(_))).filter(_.length > 0).length
  }

  case class TweetMetrics(val urlsInTweet: Option[Seq[UrlDetails]],
                          val hashTagsInTweet: Option[Seq[HashTag]],
                          val emojisInTweet: Option[Seq[Emoji]])
}



