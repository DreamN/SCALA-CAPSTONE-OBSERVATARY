package observatory

import scala.collection.mutable
import scala.collection.parallel.ParIterable

/**
  * 4th milestone: value-added information
  */
object Manipulation {

  val memoGridTemperature = new mutable.HashMap[(Int, Int), Temperature]()
  val memoAvgGridTemperature = new mutable.HashMap[(Int, Int), Temperature]()
  val memoDevGridTemperature = new mutable.HashMap[(Int, Int), Temperature]()

  /**
    * @param temperatures Known temperatures
    * @return A function that, given a latitude in [-89, 90] and a longitude in [-180, 179],
    *         returns the predicted temperature at this location
    */
  def makeGrid(temperatures: Iterable[(Location, Temperature)]): GridLocation => Temperature = {
    grid:GridLocation => {
      memoGridTemperature.getOrElseUpdate((grid.lat, grid.lon),
        Visualization.predictTemperature(temperatures, Location(grid.lat, grid.lon)))
    }
  }

  /**
    * @param temperaturess Sequence of known temperatures over the years (each element of th     e collection
    *                      is a collection of pairs of location and temperature)
    * @return A function that, given a latitude and a longitude, returns the average temperature at this location
    */
  def average(temperaturess: Iterable[Iterable[(Location, Temperature)]]): GridLocation => Temperature = {
    grid:GridLocation => {
      memoAvgGridTemperature.getOrElseUpdate((grid.lat, grid.lon), {
        val temperaturesList: ParIterable[Temperature] = temperaturess.par.map(temperatures => makeGrid(temperatures)(grid))
        temperaturesList.sum/temperaturesList.size
      })
    }
  }

  /**
    * @param temperatures Known temperatures
    * @param normals A grid containing the “normal” temperatures
    * @return A grid containing the deviations compared to the normal temperatures
    */
  def deviation(temperatures: Iterable[(Location, Temperature)], normals: GridLocation => Temperature): GridLocation => Temperature = {
    grid:GridLocation => {
      memoDevGridTemperature.getOrElseUpdate((grid.lat, grid.lon), {
        makeGrid(temperatures)(grid) - normals(grid)
      })
    }
  }


}

