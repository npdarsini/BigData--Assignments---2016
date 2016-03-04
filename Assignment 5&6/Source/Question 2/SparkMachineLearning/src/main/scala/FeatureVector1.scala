package edu.umkc.fv

import edu.umkc.fv.NLPUtils._
import edu.umkc.fv.Utils._
import org.apache.spark.SparkConf
import org.apache.spark.mllib.classification.{NaiveBayes, NaiveBayesModel}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.twitter.TwitterUtils

object FeatureVector1 {

   def main(args: Array[String]) {
     val sparkConf = new SparkConf().setMaster("local[*]").setAppName("Spark-Machine_Learning-Text-1").set("spark.driver.memory", "3g").set("spark.executor.memory", "3g")
     val ssc = new StreamingContext(sparkConf, Seconds(2))

     val filters = args


     System.setProperty("twitter4j.oauth.consumerKey", "nk7lCf9Z2qJlVhkN8QGBOkdTF")
     System.setProperty("twitter4j.oauth.consumerSecret", "1nuJjD1hGIfAfnLZ5C2PNO4Sisg9OpGOcDG0ZMehpJIbHjzIps")
     System.setProperty("twitter4j.oauth.accessToken", "340731816-VqiIbfJHsJzB698A3FpGnH2Eqv2vqS0NJGOGGenI")
     System.setProperty("twitter4j.oauth.accessTokenSecret", "RHhDJiynBr45EUUrAjOc5j70eFGFjsaMXr7GlvNVQsyEF")


    val stream = TwitterUtils.createStream(ssc, None, filters)
     stream.print()
     val hashTags = stream.flatMap(status => status.getText.split(" ").filter(_.startsWith("#")))
     val topCounts30 = hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(30))
       .map{case (topic, count) => (count, topic)}
       .transform(_.sortByKey(false))
     topCounts30.foreachRDD(rdd => {
       val topList = rdd.take(10)
       println("\nPopular topics in last 30 seconds (%s total):".format(rdd.count()))
       topList.foreach{case (count, tag) => println("%s (%s tweets)".format(tag, count))}
       rdd.saveAsSequenceFile("oup1")
     })


     val sc = ssc.sparkContext
     val stopWords = sc.broadcast(loadStopWords("/stopwords.txt")).value
     val labelToNumeric = createLabelMap("data/training/")
     var model: NaiveBayesModel = null
     // Training the data
     val training = sc.wholeTextFiles("data/training/*")
       .map(rawText => createLabeledDocument(rawText, labelToNumeric, stopWords))
     val X_train = tfidfTransformer(training)
     X_train.foreach(vv => println(vv))

     model = NaiveBayes.train(X_train, lambda = 1.0)

     val lines=sc.wholeTextFiles("oup1/*")
     val data = lines.map(line => {

         val test = createLabeledDocumentTest(line._2, labelToNumeric, stopWords)
         println(test.body)
         test


     })

          val X_test = tfidfTransformerTest(sc, data)

            val predictionAndLabel = model.predict(X_test)
            println("PREDICTION")
            predictionAndLabel.foreach(x => {
              labelToNumeric.foreach { y => if (y._2 == x) {
                println(y._1)
              }
              }
            })

     ssc.start()

     ssc.awaitTerminationOrTimeout(300)

   }

 }