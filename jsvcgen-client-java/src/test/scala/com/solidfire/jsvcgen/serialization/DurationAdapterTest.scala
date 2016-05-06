package com.solidfire.jsvcgen.serialization

import com.google.gson.{ GsonBuilder, JsonPrimitive }
import org.joda.time.Duration
import org.scalatest.{ Matchers, WordSpec }

class DurationAdapterTest extends WordSpec with Matchers {

  private def hours( x: Long ) = x * 60 * 60 * 1000

  private def minutes( x: Long ) = x * 60 * 1000

  private def seconds( x: Long ) = x * 1000

  private def milliseconds( x: Long ) = x

  private val wellFormedDurations = Map(
    "01:00:00.000000" -> new Duration( hours( 1 ) ),
    "17:00:00.000000" -> new Duration( hours( 17 ) ),
    "08:19:24.000000" -> new Duration( hours( 8 ) + minutes( 19 ) + seconds( 24 ) ),
    "00:06:06.000000" -> new Duration( minutes( 5 ) + seconds( 66 ) ),
    "01:00:00.090000" -> new Duration( hours( 1 ) + milliseconds( 90 ) ),
    "00:01:05.600000" -> new Duration( minutes( 1 ) + seconds( 5 ) + milliseconds( 600 ) ),
    "00:00:00.100000" -> new Duration( milliseconds( 100 ) ),
    "00:00:55.550000" -> new Duration( seconds( 55 ) + milliseconds( 550 ) ),
    "-01:00:00.000000" -> new Duration( -hours( 1 ) ),
    "-17:00:00.000000" -> new Duration( -hours( 17 ) ),
    "-08:19:24.000000" -> new Duration( -(hours( 8 ) + minutes( 19 ) + seconds( 24 )) ),
    "-00:06:06.000000" -> new Duration( -(minutes( 5 ) + seconds( 66 )) ),
    "-01:00:00.090000" -> new Duration( -(hours( 1 ) + milliseconds( 90 )) ),
    "-00:01:05.600000" -> new Duration( -(minutes( 1 ) + seconds( 5 ) + milliseconds( 600 )) ),
    "-00:00:00.100000" -> new Duration( -(milliseconds( 100 )) ),
    "-00:00:55.550000" -> new Duration( -(seconds( 55 ) + milliseconds( 550 )) )
  )

  private val malformedDurations = Map(
    "00:00:00.1" -> new Duration( milliseconds( 100 ) ),
    "00:00:00.100" -> new Duration( milliseconds( 100 ) ),
    "00:00:00.1000" -> new Duration( milliseconds( 100 ) ),
    "00:00:01.1000" -> new Duration( milliseconds( 1100 ) ),
    "11:10:01" -> new Duration( hours( 11 ) + minutes( 10 ) + seconds( 1 ) ),
    "00:00.0000" -> new Duration( milliseconds( 0 ) ),
    "00:00.010" -> new Duration( milliseconds( 10 ) ),
    "00:10.0000" -> new Duration( seconds( 10 ) ),
    "10:00.0000" -> new Duration( minutes( 10 ) ),
    "00:00" -> new Duration( seconds( 0 ) ),
    "00:10" -> new Duration( seconds( 10 ) ),
    "10:00" -> new Duration( minutes( 10 ) ),
    "24:24" -> new Duration( minutes( 24 ) + seconds( 24 ) ),
    "00:90" -> new Duration( minutes( 1 ) + seconds( 30 ) ),
    "01:30" -> new Duration( minutes( 1 ) + seconds( 30 ) ),
    "0" -> new Duration( seconds( 0 ) ),
    "1" -> new Duration( seconds( 1 ) ),
    "1.0" -> new Duration( seconds( 1 ) ),
    "30" -> new Duration( seconds( 30 ) ),
    "30.1" -> new Duration( seconds( 30 ) + milliseconds( 100 ) ),
    "30.011" -> new Duration( seconds( 30 ) + milliseconds( 11 ) ),
    "10.11" -> new Duration( seconds( 10 ) + milliseconds( 110 ) ),
    "-11:10:01" -> new Duration( -(hours( 11 ) + minutes( 10 ) + seconds( 1 )) ),
    "-00:00.002" -> new Duration( -milliseconds( 2 ) ),
    "-00:02.0" -> new Duration( -seconds( 2 ) ),
    "-02:00.0" -> new Duration( -minutes( 2 ) ),
    "-02:02.002" -> new Duration( -(minutes( 2 ) + seconds( 2 ) + milliseconds( 2 )) ),
    "-00:10" -> new Duration( -seconds( 10 ) ),
    "-10:00" -> new Duration( -minutes( 10 ) ),
    "-10.11" -> new Duration( -(seconds( 10 ) + milliseconds( 110 )) ),
    "-1.1" -> new Duration( -(seconds( 1 ) + milliseconds( 100 )) ),
    "-0" -> new Duration( seconds( 0 ) )
  )

  private val gson = new GsonBuilder( )
    .registerTypeAdapter( DurationAdapter.serializingClass( ), new DurationAdapter( ) )
    .create( )

  "DurationAdapter" should {
    "encode values" in {
      for ((expectedRepr, duration) <- wellFormedDurations) {
        val repr = gson.toJsonTree( duration, classOf[Duration] ).getAsJsonPrimitive( ).getAsString( )
        repr should be( expectedRepr )
      }
    }
    "decode values" in {
      for ((inputRepr, expectedDuration) <- wellFormedDurations ++ malformedDurations) {
        //      println(inputRepr)
        val duration = gson.fromJson( new JsonPrimitive( inputRepr ), classOf[Duration] )
        duration should be( expectedDuration )
      }
    }
  }
}