package observatory

import java.time.LocalDate

import org.scalatest.FunSuite
import org.scalatest.prop.Checkers

import scala.collection.concurrent.TrieMap



trait InteractionTest extends FunSuite with Checkers {

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ns")
    result
  }

  val colors: Iterable[(Temperature, Color)] = Iterable(
    ( 60, Color(255, 255, 255)),
    ( 32, Color(255,   0,   0)),
    ( 12, Color(255, 255,   0)),
    (  0, Color(  0, 255, 255)),
    (-15, Color(  0,   0, 255)),
    (-27, Color(255,   0, 255)),
    (-50, Color( 33,   0, 107)),
    (-60, Color(  0,   0,   0))
  )

  def getYearlyDataByYear(year: Year): (Year, Iterable[(Location, Temperature)]) = {
    val tempData: Iterable[(LocalDate, Location, Temperature)] = Extraction.locateTemperatures(year,
      "/stations.csv", s"/$year.csv")
    (year, Extraction.locationYearlyAverageRecords(tempData))
  }

  def fn(year: Year, tile: Tile, data: Iterable[(Location, Temperature)]): Unit = {
    val img = Interaction.tile(data, colors, tile)
    img.output(new java.io.File(s"target/temperatures/$year/${tile.zoom}/${tile.x}-${tile.y}.png"))
  }

  //Calculate & measurements
  time{
    val yearlyData = (2014 to 2015).map(getYearlyDataByYear).view
    Interaction.generateTiles(yearlyData, fn)
  }
}