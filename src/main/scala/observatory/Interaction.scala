package observatory

import com.sksamuel.scrimage.{Image, Pixel}
import observatory.Visualization.{interpolateColor, predictTemperature}

import math._

/**
  * 3rd milestone: interactive visualization
  */
object Interaction {

  /**
    * @param tile Tile coordinates
    * @return The latitude and longitude of the top-left corner of the tile, as per http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
    */
  def tileLocation(tile: Tile): Location = {
    val n = pow(2.0, tile.zoom)
    val lat = toDegrees(atan(sinh(Pi * (1.0 - 2.0 * tile.y /n))))
    val lon = (tile.x/pow(2.0, n)) * 360.0 - 180.0
    Location(lat, lon)
  }

  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @param tile Tile coordinates
    * @return A 256×256 image showing the contents of the given tile
    */

  def tile(temperatures: Iterable[(Location, Temperature)], colors: Iterable[(Temperature, Color)], tile: Tile): Image = {

    val WIDTH = 256
    val HEIGHT = 256

    val coordinates = for{
      y <- 0 until HEIGHT//pow(2, 8).toInt
      x <- 0 until WIDTH//pow(2, 8).toInt
    }yield tileLocation(Tile(x + tile.x * WIDTH, y + tile.y * HEIGHT, 8))

    val pixels = coordinates
      .par
      .map{case Location(lat, lon) => {
        val temp = predictTemperature(temperatures, Location(lat, lon))
        val color = interpolateColor(colors, temp)
        Pixel(color.red, color.green, color.blue, 127)
      }}.toArray
    Image(WIDTH, HEIGHT, pixels)
  }

  /**
    * Generates all the tiles for zoom levels 0 to 3 (included), for all the given years.
    * @param yearlyData Sequence of (year, data), where `data` is some data associated with
    *                   `year`. The type of `data` can be anything.
    * @param generateImage Function that generates an image given a year, a zoom level, the x and
    *                      y coordinates of the tile and the data to build the image from
    */
  def generateTiles[Data](
    yearlyData: Iterable[(Year, Data)],
    generateImage: (Year, Tile, Data) => Unit
  ): Unit = {
    yearlyData.foreach(data => {
      val zoomLevels:List[Int] = List(0, 1, 2, 3)
      zoomLevels.foreach( zoomLevel =>
        for{
            x <- 0 until pow(2, zoomLevel).toInt
            y <- 0 until pow(2, zoomLevel).toInt
        }generateImage(data._1, Tile(x, y, zoomLevel), data._2)
      )
    })
  }

}
