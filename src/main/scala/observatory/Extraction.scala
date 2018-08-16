package observatory

import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.apache.spark
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.functions._


/**
  * 1st milestone: data extraction
  */
object Extraction {
  import org.apache.log4j.{Level, Logger}
  //lazy val logger: Logger = Logger.getLogger("org.apache.spark")
  //logger.setLevel(Level.ALL)
  val spark: SparkSession = SparkSession
    .builder()
    .appName("Extraction")
    .config("spark.master", "local")
    .getOrCreate()

  /** @return The filesystem path of the given resource */
  def fsPath(resource: String): String =
    Paths.get(getClass.getResource(resource).toURI).toString

  def getResultOrNull[T](str: String, fn: String => T) ={
    if(str == "") null else fn(str)
  }

  def GetStationsRddFromFile(stationsFile: String): RDD[Row] = {

    def stationsRow(line: List[String]): Row = Row.fromSeq(Seq(
      line.head, line(1),
      getResultOrNull(line(2), _.toDouble),
      getResultOrNull(line(3), _.toDouble)
    ))

    spark.sparkContext.textFile(fsPath(stationsFile))
      .map(_.split(",", -1).to[List])
      .map(stationsRow)
  }

  def GetTemperatureRddFromFile(temperatureFile: String, year: Year): RDD[Row] = {

    def temperatureRow(line: List[String]): Row = Row.fromSeq(Seq(
      line.head, line(1), year.toInt,
      getResultOrNull(line(2), _.toInt),
      getResultOrNull(line(3), _.toInt),
      getResultOrNull(line(4), x => (x.toDouble - 32.0) * (5.0/9.0))
    ))
    spark.sparkContext.textFile(fsPath(temperatureFile))
      .map(_.split(",", -1).to[List])
      .map(temperatureRow)
  }


  def GetStationsDfFromFile(stationsFile: String): DataFrame = {
    val stationsRdd = GetStationsRddFromFile(stationsFile)
    val stationsSchema: StructType = StructType(
      List(
        StructField("stnIdentifier", StringType, nullable = true),
        StructField("wbanIdentifier", StringType, nullable = true),
        StructField("lat", DoubleType, nullable = true),
        StructField("lon", DoubleType, nullable = true)
      )
    )
    spark.createDataFrame(stationsRdd, stationsSchema)
  }

  def GetStationsDfFromFileFilteredNullLocation(stationsFile: String): DataFrame = {
    val df: DataFrame = GetStationsDfFromFile(stationsFile)
    df.filter("lat is not null and lon is not null")
  }

  def GetTemperatureDfFromFile(temperatureFile: String, year: Year): DataFrame = {
    val temperatureRdd = GetTemperatureRddFromFile(temperatureFile, year)
    val temperatureSchema: StructType = StructType(
      List(
        StructField("stnIdentifier", StringType, nullable = true),
        StructField("wbanIdentifier", StringType, nullable = true),
        StructField("year", IntegerType, nullable = true),
        StructField("month", IntegerType, nullable = true),
        StructField("day", IntegerType, nullable = true),
        StructField("temperature", DoubleType, nullable = true)
      )
    )

    spark.createDataFrame(temperatureRdd, temperatureSchema)
  }
  /**
    * @param year             Year number
    * @param stationsFile     Path of the stations resource file to use (e.g. "/stations.csv")
    * @param temperaturesFile Path of the temperatures resource file to use (e.g. "/1975.csv")
    * @return A sequence containing triplets (date, location, temperature)
    */

  def locateTemperatures(year: Year, stationsFile: String, temperaturesFile: String): Iterable[(LocalDate, Location, Temperature)] = {
    import spark.implicits._

    val stationsDf: DataFrame = GetStationsDfFromFileFilteredNullLocation(stationsFile)
    val temperatureDF: DataFrame = GetTemperatureDfFromFile(temperaturesFile, year.toInt)

    val joinedDf = temperatureDF.join(stationsDf,
      Seq("stnIdentifier", "wbanIdentifier"),
      "inner"
    )

    val result = joinedDf
      .select(Seq("year", "month", "day", "lat", "lon", "temperature").map(c => col(c)): _*)
      .rdd.map(r => (
        LocalDate.of(r(0).toString.toInt, r(1).toString.toInt, r(2).toString.toInt),
        Location(r(3).toString.toDouble, r(4).toString.toDouble),
        r(5).toString.toDouble
    )).collect().toIterable
    result
  }

  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Iterable[(LocalDate, Location, Temperature)]): Iterable[(Location, Temperature)] = {

    import spark.implicits._

    def avgIterable[T](iterable: Iterable[T], fn: T => Double) : Double = {
      val (sum, length): (Double, Int) = iterable.foldLeft((0.0, 0))((t, r) => (t._1 + fn(r), t._2 + 1))
      (sum/length)
    }

    def row(line: (LocalDate, Location, Temperature)): Row = Row.fromSeq(Seq(
      line._2.lat,
      line._2.lon,
      line._3.toDouble
    ))

    val schema: StructType = StructType(
      List(
        StructField("lat", DoubleType, nullable = true),
        StructField("lon", DoubleType, nullable = true),
        StructField("temperature", DoubleType, nullable = true)
      )
    )

    val rdd : RDD[Row] = spark.sparkContext.makeRDD(records.toSeq).map(row)
    val df : DataFrame = spark.createDataFrame(rdd, schema)

    df
      .groupBy("lat", "lon")
      .agg(avg("temperature").as("avgTemperature"))
      .select("lat", "lon", "avgTemperature")
      .rdd.map(r => (
      Location(r(0).toString.toDouble, r(1).toString.toDouble),
      r(2).toString.toDouble
    )).collect().toIterable
  }

}
