package observatory

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

trait ExtractionTest extends FunSuite {
  val x = Extraction.locateTemperatures(1975, "/stations.csv", "/1975.csv")
}