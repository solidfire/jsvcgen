package com.solidfire.jsvcgen.client

import java.io.StringReader
import java.net.URL
import javax.net.ssl.{HostnameVerifier, HttpsURLConnection, SSLSession}

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.stream.JsonReader
import com.google.gson.{Gson, JsonObject, JsonParser}
import com.solidfire.jsvcgen.serialization.GsonUtil
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, Matchers, WordSpec}

object WiremockSetup {
  HttpsURLConnection.setDefaultHostnameVerifier( new HostnameVerifier {
    override def verify( s: String, sslSession: SSLSession ): Boolean = true
  } )

  val keystorePath = this.getClass.getClassLoader.getResource( "keystore" ).getPath

  System.setProperty( "javax.net.ssl.trustStore", keystorePath )
}

trait WiremockSetup

class ServiceBaseSuite extends WordSpec with BeforeAndAfterAll with MockitoSugar with Matchers with WiremockSetup {


  val mockJsonObject = new JsonParser( ).parse( "{ error : { message : \"anErrorMessage\" } }" ).getAsJsonObject

  val Port               = 8443
  val Host               = "localhost"
  val Path               = "/rpc-json/7.0"
  val wireMockServer     = new WireMockServer( wireMockConfig( ).httpsPort( Port ).keystorePath( WiremockSetup.keystorePath ) )
  val _url               = new URL( s"https://$Host:$Port$Path" )
  val _requestDispatcher = new HttpsRequestDispatcher( _url )
  val _serviceBase       = new ServiceBase( _requestDispatcher )


  override def beforeAll = {
    wireMockServer.start( )
  }

  override def afterAll = {
    wireMockServer.stop( )
  }

  "sendRequest" should {

    "return a result when request succeeds" in {

      stubFor(
        post( urlEqualTo( Path ) )
          .withRequestBody( containing( "aMethod1" ) )
          .willReturn( aResponse.withHeader( "Content-Type", "application/json" )
            .withBody( "{ 'result': {} }" )
            .withStatus( 200 )
          )
      )

      val responseObject = _serviceBase.sendRequest( "aMethod1", new Object, classOf[Object], classOf[LinkedTreeMap[_, _]] )

      responseObject shouldBe a[LinkedTreeMap[_, _]]
      responseObject should have size 0
    }

    "map all response values" in {

      stubFor(
        post( urlEqualTo( Path ) )
          .withRequestBody( containing( "aMethod2" ) )
          .willReturn( aResponse.withHeader( "Content-Type", "application/json" )
            .withBody( "{'result':{'a':'b','c':'d'}}" )
            .withStatus( 200 )
          )
      )

      _serviceBase.sendRequest( "aMethod2", new Object, classOf[Object], classOf[LinkedTreeMap[_, _]] ) should have size 2
    }

    "map error message" in {

      stubFor(
        post( urlEqualTo( Path ) )
          .withRequestBody( containing( "aMethod3" ) )
          .willReturn( aResponse.withHeader( "Content-Type", "application/json" )
            .withBody( "{ error: { name: 'anErrorName', code: 500, message: 'anErrorMessage' } }" )
            .withStatus( 200 )
          )
      )

      val thrown = the[ApiServerException] thrownBy _serviceBase.sendRequest( "aMethod3", new Object, classOf[Object], classOf[LinkedTreeMap[_, _]] )

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
      val error = _serviceBase.extractApiError( convertResponseToJsonObject( "{\"name\":\"aName\", \"code\":\"aCode\", \"message\":\"aMessage\"}" ) )
      error.getName should be( "aName" )
      error.getCode should be( "aCode" )
      error.getMessage should be( "aMessage" )
    }

  }
}