package com.solidfire.jsvcgen.serialization

import com.google.gson.{ GsonBuilder, JsonPrimitive }
import org.joda.time.{ DateTime, DateTimeZone }
import org.scalatest.{ Matchers, WordSpec }

class DateTimeAdapterTest extends WordSpec with Matchers {

  private val gson = new GsonBuilder( )
    .registerTypeAdapter( DateTimeAdapter.serializingClass( ), new DateTimeAdapter( ) )
    .create( )

  "DateTimeAdapter" should {
    "decode a properly formatted Zulu time string" in {
      val input = new JsonPrimitive( "2014-09-30T18:58:45.934512Z" )
      val output = gson.fromJson( input, classOf[DateTime] )
      output.getYear should be( 2014 )
      output.getMonthOfYear should be( 9 )
      output.getDayOfMonth should be( 30 )
      output.getHourOfDay should be( 18 )
      output.getMinuteOfHour should be( 58 )
      output.getSecondOfMinute should be( 45 )
      output.getMillisOfSecond should be( 934 )
    }
    "encode a DateTime to ISO 8601 Zulu time" in {
      val input = new DateTime( 2012, 3, 14, 15, 9, 26, 535, DateTimeZone.UTC )
      val output = gson.toJsonTree( input, classOf[DateTime] ).getAsJsonPrimitive.getAsString
      output should be( "2012-03-14T15:09:26.535Z" )
    }
    "encode a non-UTC time into the equivalent UTC" in {
      val input = new DateTime( 2012, 3, 14, 20, 9, 26, 535, DateTimeZone.forOffsetHours( +5 ) )
      val output = gson.toJsonTree( input, classOf[DateTime] ).getAsJsonPrimitive.getAsString
      output should be( "2012-03-14T15:09:26.535Z" )
    }
    "encode a null time to `null`" in {
      val output = gson.toJsonTree( null, classOf[DateTime] )
      output.isJsonNull should be( true )
    }
  }
}
