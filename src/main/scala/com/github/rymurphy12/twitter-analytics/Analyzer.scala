package com.github.rymurphy12.analytics

import com.danielasfregola.twitter4s.{TwitterStreamingClient, TwitterRestClient}
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken, Tweet}
import com.danielasfregola.twitter4s.entities.streaming.StreamingMessage
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure} 


object Analyzer{
  
  def main(args: Array[String]): Unit = {
    val consumerToken = ConsumerToken(ConfigFactory.load().getString("twitter.consumer.key"), 
                                      ConfigFactory.load().getString("twitter.consumer.secret"))
    val accessToken = AccessToken(ConfigFactory.load().getString("twitter.access.key"),
                                  ConfigFactory.load().getString("twitter.access.secret"))
    
    val streamingTwitterClient = TwitterStreamingClient(consumerToken, accessToken)
    streamingTwitterClient.sampleStatuses()(updateStats)
    //val client = TwitterRestClient(consumerToken, accessToken)
    //val myTweet = client.userTimelineForUser("ryan_j_murphy12")

    //myTweet onComplete {
    //  case Success(tweets) => for (tweet <- tweets.data) println(tweet)
    //  case Failure(f) => println("You suck. here is your error: " + f.getMessage) 
   // }
  }

  def updateStats: PartialFunction[StreamingMessage, Unit] = {
    case tweet: Tweet => {
      
    }
  }
}


