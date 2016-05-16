package com.solidfire.jsvcgen.client

import java.io.StringReader

import com.google.gson.internal.LinkedTreeMap
import com.google.gson.stream.JsonReader
import com.google.gson.{Gson, JsonObject, JsonParser}
import com.solidfire.jsvcgen.JavaClasses.{Foo, FooArray, FooFoo}
import com.solidfire.jsvcgen.javautil.Optional
import com.solidfire.jsvcgen.serialization.GsonUtil
import org.mockito.Matchers.anyString
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}


class ServiceBaseSuite extends WordSpec with BeforeAndAfterAll with MockitoSugar with Matchers {

  val mockJsonObject = new JsonParser( ).parse( "{ error : { message : 'anErrorMessage' } }" ).getAsJsonObject

  val Port               = 9999
  val Host               = "localhost"
  val Path               = "/rpc-json/7.0"
  val _requestDispatcher = mock[RequestDispatcher]
  val _serviceBase       = new ServiceBase( _requestDispatcher )


  "sendRequest" should {

    "return a result when request succeeds" in {
      when( _requestDispatcher.dispatchRequest( anyString ) ).thenReturn( "{ 'result': {} }" )

      val responseObject = _serviceBase.sendRequest( "aMethod", new Object, classOf[Object], classOf[LinkedTreeMap[_, _]] )

      responseObject shouldBe a[LinkedTreeMap[_, _]]
      responseObject should have size 0
    }

    "map all response values" in {
      when( _requestDispatcher.dispatchRequest( anyString ) ).thenReturn( "{'result':{'a':'b','c':'d'}}" )

      _serviceBase.sendRequest( "aMethod", new Object, classOf[Object], classOf[LinkedTreeMap[_, _]] ) should have size 2
    }

    "map empty response values" in {
      when( _requestDispatcher.dispatchRequest( anyString ) ).thenReturn( "{'result':{'a':'','c':''}}" )

      _serviceBase.sendRequest( "aMethod", new Object, classOf[Object], classOf[LinkedTreeMap[_, _]] ) should have size 2
    }

    "map empty response values as empty" in {
      when( _requestDispatcher.dispatchRequest( anyString ) ).thenReturn( "{'result':{'a':'','c':''}}" )

      _serviceBase.sendRequest( "aMethod", new Object, classOf[Object], classOf[LinkedTreeMap[String, Object]] ).get( "a" ) should not be null
    }

    "map empty response values as empty with an object" in {
      when( _requestDispatcher.dispatchRequest( anyString ) ).thenReturn( "{'result': { 'bar':'', 'baz':'' } }" )

      val myFoo = _serviceBase.sendRequest( "aMethod", new Object, classOf[Object], classOf[Foo] )

      myFoo.getBar should not be null
      myFoo.getBar should be( "" )
    }

    "map empty optional string response values as optional empty string \"\"" in {
      when( _requestDispatcher.dispatchRequest( anyString ) ).thenReturn( "{'result': { 'bar':'', 'baz':'' } }" )

      val myFoo = _serviceBase.sendRequest( "aMethod", new Object, classOf[Object], classOf[Foo] )

      myFoo.getBaz should not be null
      myFoo.getBaz should be( Optional.of("") )
    }

    "map null optional response values as empty in non-null objects with a completely empty complex object" in {
      when( _requestDispatcher.dispatchRequest( anyString ) ).thenReturn( "{'result': { } }" )

      val myFoo = _serviceBase.sendRequest( "aMethod", new Object, classOf[Object], classOf[FooFoo] )

      myFoo.getBar shouldBe null
      myFoo.getBaz should not be null
      myFoo.getBaz should be( Optional.empty( ) )
    }

    "map null optional response values as empty with a complex object" in {
      when( _requestDispatcher.dispatchRequest( anyString ) ).thenReturn( "{'result': { 'bar':{}, 'baz': null } }" )

      val myFoo = _serviceBase.sendRequest( "aMethod", new Object, classOf[Object], classOf[FooFoo] )

      myFoo.getBar should not be null
      myFoo.getBaz should not be null
      myFoo.getBar.getBar shouldBe null
      myFoo.getBar.getBaz should not be null
      myFoo.getBar.getBaz should be( Optional.empty( ) )
      myFoo.getBaz should be( Optional.empty( ) )
    }

    "map null optional response values as empty with an all null complex object" in {
      when( _requestDispatcher.dispatchRequest( anyString ) ).thenReturn( "{'result': { 'bar':{ 'bar':null, 'baz': null } }, 'baz': null } }" )

      val myFoo = _serviceBase.sendRequest( "aMethod", new Object, classOf[Object], classOf[FooFoo] )

      myFoo.getBar should not be null
      myFoo.getBaz should not be null
      myFoo.getBar.getBar shouldBe null
      myFoo.getBar.getBaz should not be null
      myFoo.getBar.getBaz should be( Optional.empty( ) )
      myFoo.getBaz should be( Optional.empty( ) )
    }

    "map array of null optional response values as empty with an all null complex object" in {
      when( _requestDispatcher.dispatchRequest( anyString ) ).thenReturn( "{'result': { 'bar': [{ 'bar':null, 'baz': null } ], 'baz': null } }" )

      val myFoo = _serviceBase.sendRequest( "aMethod", new Object, classOf[Object], classOf[FooArray] )

      myFoo.getBar should not be null
      myFoo.getBaz should not be null
      myFoo.getBar( )( 0 ).getBar shouldBe null
      myFoo.getBar( )( 0 ).getBaz should not be null
      myFoo.getBar( )( 0 ).getBaz should be( Optional.empty( ) )
      myFoo.getBaz should be( Optional.empty( ) )
    }

    "map error message" in {
      when( _requestDispatcher.dispatchRequest( anyString ) ).thenReturn( "{ error: { name: 'anErrorName', code: 500, message: 'anErrorMessage' } }" )

      val thrown = the[ApiServerException] thrownBy _serviceBase.sendRequest( "aMethod", new Object, classOf[Object], classOf[LinkedTreeMap[_, _]] )

      thrown should not be null
      thrown.getName shouldBe "anErrorName"
      thrown.getCode shouldBe "500"
      thrown.getMessage shouldBe "anErrorMessage"
    }

    "throw exception when method is null" in {
      a[IllegalArgumentException] should be thrownBy {
        _serviceBase.sendRequest( null, AnyRef, classOf[AnyRef], classOf[AnyRef] )
      }
    }

    "throw exception when method is empty" in {
      a[IllegalArgumentException] should be thrownBy {
        _serviceBase.sendRequest( "", AnyRef, classOf[AnyRef], classOf[AnyRef] )
      }
    }


    "throw exception when request parameter is null" in {
      a[IllegalArgumentException] should be thrownBy {
        _serviceBase.sendRequest( "method", null, classOf[AnyRef], classOf[AnyRef] )
      }
    }

    "throw exception when request parameter class is null" in {
      a[IllegalArgumentException] should be thrownBy {
        _serviceBase.sendRequest( "method", AnyRef, null, classOf[AnyRef] )
      }
    }

    "throw exception when result parameter class is null" in {
      a[IllegalArgumentException] should be thrownBy {
        _serviceBase.sendRequest( "method", AnyRef, classOf[AnyRef], null )
      }
    }


  }

  "encodeRequest" should {

    "throw exception when method is null" in {
      a[IllegalArgumentException] should be thrownBy {
        _serviceBase.encodeRequest( null, AnyRef, classOf[AnyRef] )
      }
    }

    "throw exception when method is empty" in {
      a[IllegalArgumentException] should be thrownBy {
        _serviceBase.encodeRequest( "", AnyRef, classOf[AnyRef] )
      }
    }

    "throw exception when request parameter is null" in {
      a[IllegalArgumentException] should be thrownBy {
        _serviceBase.encodeRequest( "method", null, classOf[AnyRef] )
      }
    }

    "throw exception when request parameter class is null" in {
      a[IllegalArgumentException] should be thrownBy {
        _serviceBase.encodeRequest( "method", AnyRef, null )
      }
    }
  }

  "decodeResponse" should {
    "throw apiException when the response is null" in {
      the[ApiException] thrownBy {
        _serviceBase.decodeResponse( null, classOf[Any] )
      } should have message "There was a problem parsing the response from the server. ( response=null )"
    }
    "throw apiException when the response is empty" in {
      the[ApiException] thrownBy {
        _serviceBase.decodeResponse( "", classOf[Any] )
      } should have message "There was a problem parsing the response from the server. ( response= )"
    }
    "throw apiException when the response is not json" in {
      the[ApiException] thrownBy {
        _serviceBase.decodeResponse( "I Cause Errors", classOf[Any] )
      } should have message "There was a problem parsing the response from the server. ( response=I Cause Errors )"
    }
  }

  def convertResponseToJsonObject( response: String ): JsonObject = {
    val gson: Gson = GsonUtil.getDefaultBuilder.create
    val reader: JsonReader = new JsonReader( new StringReader( response ) )
    reader.setLenient( true )

    gson.fromJson( reader, classOf[JsonObject] )
  }

  "extractApiError" should {
    "always return a non null instance" in {
      _serviceBase.extractApiError( convertResponseToJsonObject( "{}" ) ) should not be null
    }
    "map fields to exception" in {
      val error = _serviceBase.extractApiError( convertResponseToJsonObject( "{'name':'aName', 'code':'aCode', 'message':'aMessage'}" ) )
      error.getName should be( "aName" )
      error.getCode should be( "aCode" )
      error.getMessage should be( "aMessage" )
    }

  }
}