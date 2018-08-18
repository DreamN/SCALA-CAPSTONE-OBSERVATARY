package observatory


import java.text.SimpleDateFormat
import java.time.LocalDate

import org.scalatest.FunSuite
import org.scalatest.prop.Checkers

trait VisualizationTest extends FunSuite with Checkers {
  val points: Iterable[(Temperature, Color)] = Iterable(
    ( 60, Color(255, 255, 255)),
    ( 32, Color(255,   0,   0)),
    ( 12, Color(255, 255,   0)),
    (  0, Color(  0, 255, 255)),
    (-15, Color(  0,   0, 255)),
    (-27, Color(255,   0, 255)),
    (-50, Color( 33,   0, 107)),
    (-60, Color(  0,   0,   0))
  )
//  println(Visualization.interpolateColor(points, 5))
//  println(Visualization.interpolateColor(points, -335))
//  println(Visualization.interpolateColor(points, 185))
//  println(Visualization.interpolateColor(points, 15))
//  println(Visualization.interpolateColor(points, -20))
//  println(Visualization.interpolateColor(points, 0))
  val tempData: Iterable[(LocalDate, Location, Temperature)] = Extraction.locateTemperatures(2006, "/stations.csv", "/2006.csv")
  val yearlyAvgTemp: Iterable[(Location, Temperature)] = Extraction.locationYearlyAverageRecords(tempData)
  val tempX = Visualization.predictTemperature(yearlyAvgTemp, Location(55.23, 23.45))
  val colorX = Visualization.interpolateColor(points, tempX)
  val img = Visualization.visualize(yearlyAvgTemp, points)
  val now = new java.util.Date()
  img.output(new java.io.File(s"target/some${
    now + "t" + now.getTime()
  }.png"))
  println("done..")
}
