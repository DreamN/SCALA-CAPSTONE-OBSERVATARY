package observatory

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

trait ExtractionTest extends FunSuite {
  val x = Extraction.locateTemperatures(2000, "/stationsTest.csv", "/2000Test.csv")
  val y: Iterable[_] = Extraction.locationYearlyAverageRecords(x)
  println(s"done!.. -> ${y.size} ")
}