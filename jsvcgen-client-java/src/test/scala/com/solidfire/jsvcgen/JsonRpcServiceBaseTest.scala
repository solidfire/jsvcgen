package com.solidfire.jsvcgen


import java.io.{ ByteArrayInputStream, IOException, InputStream }
import java.net.{ HttpURLConnection, ProtocolException, URL }
import java.util.concurrent.TimeUnit

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.google.gson.JsonParser
import com.google.gson.internal.LinkedTreeMap
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfter, FlatSpec, Matchers }

import dispatch.{ Http, url }

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
 * Created by Jason Ryan Womack on 6/10/15.
 */
class JsonRpcServiceBaseTest extends FlatSpec with BeforeAndAfter with MockitoSugar with Matchers {
  val mockConnection = mock[HttpURLConnection]

  val mockJsonObject = new JsonParser( ).parse( "{ error : { message : \"anErrorMessage\" } }" ).getAsJsonObject

  val Port           = 9191
  val Host           = "localhost"
  val wireMockServer = new WireMockServer( wireMockConfig( ).port( Port ) )

  val testObject = new JsonRpcServiceBase( new URL( s"http://$Host:$Port/rpc-json" ) )

  before {
    when( mockConnection.getResponseCode ).thenReturn( 200 )
    when( mockConnection.getInputStream ).thenReturn( mock[InputStream] )
    when( mockConnection.getErrorStream ).thenReturn( mock[InputStream] )

    wireMockServer.start( )
    WireMock.configureFor( Host, Port )
  }

  after {
    reset( mockConnection )
    wireMockServer.stop( )
  }

  "prepareConnection" should "throw exception when connection is null" in {
    a[NullPointerException] should be thrownBy {
      JsonRpcServiceBase.prepareConnection( null )
    }
  }

  "prepareConnection" should "set connection request method to POST" in {
    JsonRpcServiceBase.prepareConnection( mockConnection )

    verify( mockConnection ).setRequestMethod( "POST" )
    verify( mockConnection ).setDoOutput( true )
    verify( mockConnection ).addRequestProperty( "Accept", "application/json" )
    verifyNoMoreInteractions( mockConnection )
  }

  "prepareConnection" should "throw Runtime Exception if request method is not supported" in {
    a[RuntimeException] should be thrownBy {
      when( mockConnection.setRequestMethod( anyString ) ).thenThrow( new ProtocolException )
      JsonRpcServiceBase.prepareConnection( mockConnection )

      verify( mockConnection ).setRequestMethod( "POST" )
      verifyNoMoreInteractions( mockConnection )
    }
  }



  "sendRequest" should "throw exception when method is null" in {
    a[IllegalArgumentException] should be thrownBy {
      testObject.sendRequest( null, AnyRef, AnyRef.getClass, AnyRef.getClass )
    }
  }

  "sendRequest" should "throw exception when method is empty" in {
    a[IllegalArgumentException] should be thrownBy {
      testObject.sendRequest( "", AnyRef, AnyRef.getClass, AnyRef.getClass )
    }
  }


  "sendRequest" should "throw exception when request parameter is null" in {
    a[IllegalArgumentException] should be thrownBy {
      testObject.sendRequest( "method", null, AnyRef.getClass, AnyRef.getClass )
    }
  }

  "sendRequest" should "throw exception when request parameter class is null" in {
    a[IllegalArgumentException] should be thrownBy {
      testObject.sendRequest( "method", AnyRef, null, AnyRef.getClass )
    }
  }

  "sendRequest" should "throw exception when result parameter class is null" in {
    a[IllegalArgumentException] should be thrownBy {
      testObject.sendRequest( "method", AnyRef, AnyRef.getClass, null )
    }
  }

  "sendRequest" should "return a result when request succeeds" in {

    val path = "/rpc-json"
    stubFor(post(urlEqualTo(path)).willReturn(aResponse().withBody("{ }").withStatus(200)))

    val request = url(s"http://$Host:$Port$path").POST
    val responseFuture = Http(request)

    val responseObject = testObject.sendRequest( "aMethod", new Object, classOf[Object], classOf[LinkedTreeMap[_,_]] )

    responseObject shouldBe a[LinkedTreeMap[_,_]]
    responseObject should have size 0

    val response = Await.result(responseFuture, Duration(100, TimeUnit.MILLISECONDS))
    response.getStatusCode should be(200)
  }

  "sendRequest" should "map all response values" in {

    val path = "/rpc-json"
    stubFor(post(urlEqualTo(path)).willReturn(aResponse().withBody("{ 'a':'b', 'c':'d' }").withStatus(200)))

    val request = url(s"http://$Host:$Port$path").POST
    val responseFuture = Http(request)

    testObject.sendRequest( "aMethod", new Object, classOf[Object], classOf[LinkedTreeMap[_,_]] ) should have size 2

    val response = Await.result(responseFuture, Duration(100, TimeUnit.MILLISECONDS))
    response.getStatusCode should be(200)
  }

  "sendRequest" should "map error message" in {

    val path = "/rpc-json"
    stubFor(post(urlEqualTo(path)).willReturn(aResponse().withBody("{ error: { message: \"anErrorMessage\" } }").withStatus(500)))

    val request = url(s"http://$Host:$Port$path").POST
    val responseFuture = Http(request)

    testObject.sendRequest( "aMethod", new Object, classOf[Object], classOf[LinkedTreeMap[_,_]] ).get("error").asInstanceOf[LinkedTreeMap[String,_]].get("message") shouldBe "anErrorMessage"

    val response = Await.result(responseFuture, Duration(100, TimeUnit.MILLISECONDS))
    response.getStatusCode should be(500)
  }

  "encodeRequest" should "throw exception when method is null" in {
    a[IllegalArgumentException] should be thrownBy {
      testObject.encodeRequest( null, AnyRef, AnyRef.getClass )
    }
  }

  "encodeRequest" should "throw exception when method is empty" in {
    a[IllegalArgumentException] should be thrownBy {
      testObject.encodeRequest( "", AnyRef, AnyRef.getClass )
    }
  }


  "encodeRequest" should "throw exception when request parameter is null" in {
    a[IllegalArgumentException] should be thrownBy {
      testObject.encodeRequest( "method", null, AnyRef.getClass )
    }
  }

  "encodeRequest" should "throw exception when request parameter class is null" in {
    a[IllegalArgumentException] should be thrownBy {
      testObject.encodeRequest( "method", AnyRef, null )
    }
  }



  "decodeResponse" should "throw exception when method is null" in {
    a[IllegalArgumentException] should be thrownBy {
      testObject.decodeResponse( null, AnyRef.getClass )
    }
  }

  "decodeResponse" should "throw exception when request parameter class is null" in {
    a[IllegalArgumentException] should be thrownBy {
      testObject.decodeResponse( new ByteArrayInputStream( "".getBytes( "UTF-8" ) ), null )
    }
  }



  "getConnectionStream" should "throw RuntimeException when HttpURLConnection is null" in {
    a[NullPointerException] should be thrownBy {
      JsonRpcServiceBase.getConnectionStream( null )
    }
  }

  "getConnectionStream" should "return the connection InputStream" in {
    JsonRpcServiceBase.getConnectionStream( mockConnection ) shouldBe an[InputStream]

    verify( mockConnection ).getResponseCode
    verify( mockConnection ).getInputStream
    verify( mockConnection, never ).getErrorStream
    verifyNoMoreInteractions( mockConnection )
  }

  "getConnectionStream" should "return the connection ErrorStream when Response Code is not 200" in {
    when( mockConnection.getResponseCode ).thenReturn( 500 )

    JsonRpcServiceBase.getConnectionStream( mockConnection ) shouldBe an[InputStream]

    verify( mockConnection ).getResponseCode
    verify( mockConnection, never ).getInputStream
    verify( mockConnection ).getErrorStream
    verifyNoMoreInteractions( mockConnection )
  }



  "extractErrorResponse" should "throw exception when JsonObject is null" in {
    a[NullPointerException] should be thrownBy {
      JsonRpcServiceBase.extractErrorResponse( null )
    }
  }

  "extractErrorResponse" should "return an JsonRpcException" in {
    JsonRpcServiceBase.extractErrorResponse( mockJsonObject ) shouldBe a[JsonRpcException]
  }

  "extractErrorResponse" should "extract from { error: { message: \"anErrorMessage\" } }" in {
    JsonRpcServiceBase.extractErrorResponse( mockJsonObject ).getMessage shouldBe "anErrorMessage"
  }



  "getExceptionForIOException" should "never throw exception when IOException is null" in {
    noException should be thrownBy {
      JsonRpcServiceBase.getExceptionForIOException( null ) shouldBe a[JsonRpcException]
      () // avoid non-Unit return warning
    }
  }

  "getExceptionForIOException" should "return JsonRpcException when IOException is null" in {
    JsonRpcServiceBase.getExceptionForIOException( null ) shouldBe a[JsonRpcException]
  }

  "getExceptionForIOException" should "return default JsonRpcException when IOException is null" in {
    JsonRpcServiceBase.getExceptionForIOException( null ).getMessage shouldBe "JSON-RPC exception"
  }

  "getExceptionForIOException" should "return wrapped IOException" in {
    JsonRpcServiceBase.getExceptionForIOException( new IOException ).getCause shouldBe a[IOException]
  }


  "convertStreamToString" should "throw exception when InputStream is null" in {
    a[NullPointerException] should be thrownBy {
      JsonRpcServiceBase.convertStreamToString( null )
    }
  }

  "convertStreamToString" should "return \"(empty)\" when InputStream is not null" in {
    JsonRpcServiceBase.convertStreamToString( mock[InputStream] ) shouldBe ""
  }

}
