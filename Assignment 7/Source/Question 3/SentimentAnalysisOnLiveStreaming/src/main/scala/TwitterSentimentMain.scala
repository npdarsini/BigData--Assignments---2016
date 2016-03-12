/**
  * Created by npdar on 3/11/2016.
  */

import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

object TwitterSentimentMain {
  def main(args: Array[String]) {
    val filters = args

    // Set the system properties so that Twitter4j library used by twitter stream
    // can use them to generate OAuth credentials

    System.setProperty("twitter4j.oauth.consumerKey", "nk7lCf9Z2qJlVhkN8QGBOkdTF")
    System.setProperty("twitter4j.oauth.consumerSecret", "1nuJjD1hGIfAfnLZ5C2PNO4Sisg9OpGOcDG0ZMehpJIbHjzIps")
    System.setProperty("twitter4j.oauth.accessToken", "340731816-VqiIbfJHsJzB698A3FpGnH2Eqv2vqS0NJGOGGenI")
    System.setProperty("twitter4j.oauth.accessTokenSecret", "RHhDJiynBr45EUUrAjOc5j70eFGFjsaMXr7GlvNVQsyEF")

    //Create a spark configuration with a custom name and master
    val sparkConf = new SparkConf().setAppName("Priyapp1").setMaster("local[*]")
    //Create a Streaming COntext with 2 second window
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    //Using the streaming context, open a twitter stream
    //Stream generates a series of random tweets
    val stream = TwitterUtils.createStream(ssc, None, filters)
    //  stream.print()

    val sentiment:DStream[TweetWithSentiment]=stream.map{Status=>{
      val st=Status.getText()
      val sa=new SentimentAnalyzer()
      val tw=sa.findSentiment(st)
      tw
    }}

    sentiment.foreachRDD{
      rdd=>rdd.foreach{
        tw=> {
          if(tw!=null)
            println(tw.getLine+"      "+tw.getCssClass)
        }}}
    ssc.start()

    ssc.awaitTermination()
  }

}