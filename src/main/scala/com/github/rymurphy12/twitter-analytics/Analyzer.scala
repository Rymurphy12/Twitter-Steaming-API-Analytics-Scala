package com.github.rymurphy12.analytics

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import akka.actor.ActorSystem
import com.danielasfregola.twitter4s.TwitterStreamingClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken, HashTag, Tweet, UrlDetails}
import com.danielasfregola.twitter4s.entities.streaming.StreamingMessage
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import com.vdurmont.emoji._

object Analyzer{
  val aggregatedTweetData: ListBuffer[IndividualTweetData] = new ListBuffer()
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
    schedulingSystem.scheduler.schedule(10 seconds, 15 seconds)(displayCurrentTweetMetrics(aggregatedTweetData.toList))
  }

  def collectRawTwitterData: PartialFunction[StreamingMessage, Unit] = {
    case tweet: Tweet =>  aggregatedTweetData += pullIndividualTweetData(tweet)
  }

  def pullIndividualTweetData(tweet: Tweet): IndividualTweetData = tweet.entities match {
      case None => IndividualTweetData(None, None, getEmojis(tweet.text))
      case Some(e) => IndividualTweetData(Some(e.urls), Some(e.hashtags), getEmojis(tweet.text))
  }

  def getEmojis(tweetText: String): Option[Seq[String]] = {
    val extractEmos = EmojiParser.extractEmojis(tweetText).asScala.toSeq
    if (extractEmos.length > 0) Some(extractEmos) else None
  }

  def displayCurrentTweetMetrics(rawTweetData: List[IndividualTweetData]): Unit = {
    val tweetMetricsResults = calculateCurrentTweetMetrics(rawTweetData)

    println(s"Total Number of Tweets Collected:               ${tweetMetricsResults.totalTweets}")
    println(f"Average Tweets Per Second:                      ${tweetMetricsResults.tweetsPerSecond}%2f")
    println(f"Average Tweets Per Minute:                      ${tweetMetricsResults.tweetsPerMinute}%2f")
    println(f"Percent of Tweets That Contain Emojis:          ${(tweetMetricsResults.emojiPercentage * 100)}%2f percent of tweets")
    println(f"Percentage of Tweets That Contain HashTags:     ${(tweetMetricsResults.hashTagPercentage * 100)}%2f percent of tweets")
    println(f"Percentage of Tweets That Contain a URL:        ${(tweetMetricsResults.urlPercentage * 100)}%2f precent of tweets")
    println(f"Percentage of Tweets That Contin a Picture URL: ${(tweetMetricsResults.pictureUrlPercentage * 100)}%2f percent of tweets")
    println()
    println("Top Five Emojis:")
    println(tweetMetricsResults.topFiveEmojis.mkString("\n"))
    println()
    println("Top Five HashTags:")
    println(tweetMetricsResults.topFiveHashTags.mkString("\n"))
    println()
    println("Top Domains:")
    println(tweetMetricsResults.topFiveDomains.mkString("\n"))
    println()
  }

  def calculateCurrentTweetMetrics(rawTweetData: List[IndividualTweetData]): CalculatedTweetMetrics = {
    val currentTimeInMillis = DateTime.now().getMillis

    val totalTweets = rawTweetData.length
    val tweetsPerSecond = (totalTweets / ((currentTimeInMillis - startTimeInMillis)/ 1000.0))
    val tweetsPerMinute = if (currentTimeInMillis - startTimeInMillis < 60000) tweetsPerSecond * 60
                          else totalTweets  / ((currentTimeInMillis - startTimeInMillis ) / 60000.0)
    val tweetsPerHour = if (currentTimeInMillis - startTimeInMillis < 3600000) tweetsPerMinute * 60
                        else totalTweets / ((currentTimeInMillis - startTimeInMillis ) / 3600000.0)

    val topFiveEmojis = getTopFiveEmojis(rawTweetData)
    val emojiPercentage =  (rawTweetData.map(_.emojisInTweet.getOrElse(Seq.empty))
                                             .filter(_.length > 0).length) / totalTweets.toDouble

    val topFiveHashTags = getTopFiveHashTags(rawTweetData)
    val hashTagPercentage = (rawTweetData.map(_.hashTagsInTweet.getOrElse(Seq.empty))
                                              .filter(_.length > 0).length) / totalTweets.toDouble

    val topFiveDomains = getTopFiveDomains(rawTweetData)
    val urlPercentage = (rawTweetData.map(_.urlsInTweet.getOrElse(Seq.empty))
      .filter(_.length > 0).length) / totalTweets.toDouble
    val pictureUrlPercentage = getTotalImageUrls(rawTweetData) / totalTweets.toDouble

    CalculatedTweetMetrics(totalTweets, tweetsPerSecond, tweetsPerMinute, tweetsPerHour, topFiveEmojis, emojiPercentage,
                           topFiveHashTags, hashTagPercentage, topFiveDomains, urlPercentage, pictureUrlPercentage)
  }

  def getTopFiveEmojis(twitterMetricsForEmojis: List[IndividualTweetData]): Seq[String] = {
    val allCollectedEmojis = twitterMetricsForEmojis.flatMap(_.emojisInTweet.getOrElse(Seq.empty))

    val emojiFrequencey = allCollectedEmojis.groupBy(identity).mapValues(_.length)
    val topFive = emojiFrequencey.toSeq.sortBy { case (emoji, frequency)  => -frequency }.take(5).zipWithIndex
    topFive.map{case ((emoji, frequency), rank) => s"[${rank + 1}] $emoji (found $frequency times) " }
  }

  def getTopFiveHashTags(twitterMetricsForHashTags: List[IndividualTweetData]): Seq[String] = {
    val allCollectedHashTags = twitterMetricsForHashTags.flatMap(_.hashTagsInTweet.getOrElse(Seq.empty))

    val hashTagText = allCollectedHashTags.map(_.text)
    val hashTagFrequency = hashTagText.groupBy(identity).mapValues(_.length)
    val topFive = hashTagFrequency.toSeq.sortBy { case (hashtag, frequency) => -frequency }.take(5).zipWithIndex
    topFive.map{case ((hashtag, frequency), rank) => s"[${rank + 1}] $hashtag (found $frequency times)"}
  }
  
  def getTopFiveDomains(twitterMetricsForDomains: List[IndividualTweetData]): Seq[String] = {
    val allCollectedUrls = twitterMetricsForDomains.flatMap(_.urlsInTweet.getOrElse(Seq.empty))

    val urlSimplified = allCollectedUrls.map(_.display_url.split("/").head)
    val urlFrequency = urlSimplified.groupBy(identity).mapValues(_.length)
    val topFive = urlFrequency.toSeq.sortBy { case (url, frequency) => -frequency }.take(5).zipWithIndex
    topFive.map{case ((url, frequency), rank) => s"[${rank + 1}] $url (found $frequency times)"}
  }

  def getTotalImageUrls(twitterMetricsForPicUrls: List[IndividualTweetData]): Int = {
    val listOfImageUrls = List("pic.twitter.com", "instagram.com", "flickr.com")
    val totalURls = twitterMetricsForPicUrls.map(_.urlsInTweet.getOrElse(Seq.empty))
    totalURls.map(seq => seq.map(urlDetail => urlDetail.display_url.split("/").head)
             .filter(listOfImageUrls.contains(_))).filter(_.length > 0).length
  }

  case class IndividualTweetData(val urlsInTweet: Option[Seq[UrlDetails]],
                                 val hashTagsInTweet: Option[Seq[HashTag]],
                                 val emojisInTweet: Option[Seq[String]])

  case class CalculatedTweetMetrics(val totalTweets: Int,
                                    val tweetsPerSecond: Double,
                                    val tweetsPerMinute: Double,
                                    val tweetsPerHour: Double,
                                    val topFiveEmojis: Seq[String],
                                    val emojiPercentage: Double,
                                    val topFiveHashTags: Seq[String],
                                    val hashTagPercentage: Double,
                                    val topFiveDomains: Seq[String],
                                    val urlPercentage: Double,
                                    val pictureUrlPercentage: Double)
}



