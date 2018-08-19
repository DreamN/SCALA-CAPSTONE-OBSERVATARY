package observatory

import com.sksamuel.scrimage.{Image, Pixel}
import math._

/**
  * 2nd milestone: basic visualization
  */
object Visualization {

  val P = 6
  /**
    * @param temperatures Known temperatures: pairs containing a location and the temperature at this location
    * @param location Location where to predict the temperature
    * @return The predicted temperature at `location`
    */
  def predictTemperature(temperatures: Iterable[(Location, Temperature)], location: Location): Temperature = {
    val findExistLoc = temperatures.filter(_._1.equals(location))
    if(findExistLoc.nonEmpty){
      findExistLoc.head._2
    }
    else{
      val (resNumeratorZp, resDenominatorZp) = temperatures.foldLeft(0.0, 0.0){
        case ((accNumerator, accDenominator), (iLocation, iTemperature)) => {
          val d = location.dist(iLocation)
          (accNumerator + (iTemperature/d), accDenominator + (1/d))
        }
      }
      val Zp = resNumeratorZp/resDenominatorZp
      Zp
    }
  }

  def interpolationBetweenTwoTemperature(x1: Temperature, y1: Int, x2: Temperature, y2: Int): Temperature => Int = {
    x =>
    math.round(y1 + ((x - x1)*((y2 - y1)/(x2 - x1)))).toInt
  }

  def interpolationBetweenTwoTemperature(x1: Temperature, x2: Temperature): (Int, Int) => Temperature => Int = {
    (y1:Int, y2:Int) =>
      interpolationBetweenTwoTemperature(x1, y1, x2, y2)
  }

  def interpolateBetweenTwoColor(a: (Temperature, Color), b: (Temperature, Color)): Temperature => Color = { x: Temperature =>
    val equation = interpolationBetweenTwoTemperature(a._1, b._1)
    Color(equation(a._2.red, b._2.red)(x), equation(a._2.green, b._2.green)(x), equation(a._2.blue, b._2.blue)(x))
  }

  /**
    * @param points Pairs containing a value and its associated color
    * @param value The value to interpolate
    * @return The color that corresponds to `value`, according to the color scale defined by `points`
    */
  def interpolateColor(points: Iterable[(Temperature, Color)], value: Temperature): Color = {
    val (topTL, bottomTL) = (points.filter(_._1 >= value), points.filter(_._1 <= value))
    if(topTL.isEmpty) points.maxBy(_._1)._2
    else if(bottomTL.isEmpty) points.minBy(_._1)._2
    else{
      val (topT, bottomT) = (points.filter(_._1 >= value).minBy(_._1), points.filter(_._1 <= value).maxBy(_._1))
      if(topT._1 == bottomT._1) topT._2
      else interpolateBetweenTwoColor(topT, bottomT)(value)
    }
  }

  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @return A 360×180 image where each pixel shows the predicted temperature at its location
    */
  def visualize(temperatures: Iterable[(Location, Temperature)], colors: Iterable[(Temperature, Color)]): Image = {
    val coordinates = for {
      lat <- 90 to -89 by -1//90 to -89
      lon <- -180 to 179//-180 to 179
    } yield (lat, lon)
    val pixels = coordinates.par.map{
      case (lat, lon) => {
        val temp = predictTemperature(temperatures, Location(lat, lon))
        val color = interpolateColor(colors, temp)
        Pixel(color.red, color.green, color.blue, 255)
      }
    }.toArray
    Image(360, 180, pixels)
  }
}

