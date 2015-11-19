import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.classification.RandomForestClassificationModel
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD

import java.sql.Timestamp

import com.datastax.spark.connector._

/**
 * Spark pipeline random forest implementation
 */
object RandomForestSpark {

  // Cassandra address
  var cassandraAddr = "192.168.69.1"

  // spark master address
  var sparkAddr = "local[4]"

  // main class for jar
  var mainClass = "RandomForestSpark"

  // Cassandra keyspace
  var keySpace = "test"

  // train data table
  var trainTable = "train_data"

  // test data table
  var testTable = "test_data"

  // performance metrics table
  var perfTable = "test_perf"

  def main (args: Array[String]) {
    // configures Spark
    val conf = new SparkConf(true)
      .set("spark.cassandra.connection.host", cassandraAddr)

    // connect to the Spark cluster
    val sc = new SparkContext(sparkAddr, mainClass, conf)

    // fetch data from Cassandra tables
    val rddTrain : RDD[LabeledPoint] = sc.cassandraTable(keySpace, trainTable).map { row  => { 
      (LabeledPoint(row.getInt("label").toDouble, Vectors.dense(
        row.getInt("avg_bid").toDouble, 
        row.getInt("delta_bid").toDouble, 
        row.getInt("diff_bid").toDouble, 
        row.getInt("range_bid").toDouble, 
        row.getInt("spread").toDouble)))
    }}

    val rddTest : RDD[LabeledPoint] = sc.cassandraTable(keySpace, testTable).map { row  => { 
      (LabeledPoint(row.getInt("label").toDouble, Vectors.dense(
        row.getInt("avg_bid").toDouble, 
        row.getInt("delta_bid").toDouble, 
        row.getInt("diff_bid").toDouble, 
        row.getInt("range_bid").toDouble, 
        row.getInt("spread").toDouble)))
    }}

    // Create data frames
    val sqlContext = new SQLContext(sc)
    val trainDF = sqlContext.createDataFrame(rddTrain)
    val testDF = sqlContext.createDataFrame(rddTest)

    // index label for use of RandomForestClassifier
    val indexer = new StringIndexer()
      .setInputCol("label")
      .setOutputCol("indexedLabel")

    val trees = args(0).toInt

    // random forest classifier
    val rf = new RandomForestClassifier()
      .setLabelCol("indexedLabel")
      .setPredictionCol("predictedLabel")
      .setNumTrees(args(0).toInt)

    // pipeline
    val pipeline = new Pipeline().setStages(Array(indexer, rf))

    // train the model
    val model = pipeline.fit(trainDF)

    // make predictions over test data
    val pred = model.transform(testDF)

    // calculate accuracy
    val accuracy = pred.filter("predictedLabel = indexedLabel").count.toDouble / pred.count()

    // save performance metrics (accuracy) to Cassandra
    val metricsData = sc.parallelize(Seq((System.currentTimeMillis(), trees, accuracy.toDouble)))
    metricsData.saveToCassandra(keySpace, perfTable, SomeColumns("ts", "trees", "accuracy"))
  }
}


