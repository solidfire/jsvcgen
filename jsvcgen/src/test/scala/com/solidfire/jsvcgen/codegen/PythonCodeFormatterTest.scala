package com.solidfire.jsvcgen.codegen

import com.solidfire.jsvcgen.model.{Member, TypeDefinition, TypeUse}
import org.scalatest.{Matchers, WordSpec}


class PythonCodeFormatterTest extends WordSpec with Matchers {

  "getTypeImports" should {
    "return one import statements" in {
      val typeDefinition = TypeDefinition( "test", None,
        List( Member( "SomeNumber", TypeUse( "integer" ) ),
          Member( "CustomTypeField1", TypeUse( "CustomType" ) ),
          Member( "CustomTypeField2", TypeUse( "CustomType" ) )
        )
      )
      val formatter = new PythonCodeFormatter(
        TestHelper.buildOptions.copy( namespace = "testNameSpace" ),
        TestHelper.buildServiceDefinition )
      val imports = formatter.getTypeImports( typeDefinition, Some( TestHelper.buildOptions ) )
      imports should be( "from com.example.CustomType import CustomType" )
    }
  }

  "isDirectType" should {
    "return true for direct types" in {
      val member = Member( "SomeNumber", TypeUse( "integer" ) )
      val formatter = new PythonCodeFormatter(
        TestHelper.buildOptions.copy( namespace = "testNameSpace" ),
        TestHelper.buildServiceDefinition )
      formatter.isDirectType( member ) should be( true )
    }
    "directTypes gets integer" in {
      val formatter = new PythonCodeFormatter(
        TestHelper.buildOptions.copy( namespace = "testNameSpace" ),
        TestHelper.buildServiceDefinition )
      formatter.directTypeNames.get( "integer" ).isDefined should be( true )
    }
  }

  "wrapParameterDict" should {
    val formatter = new PythonCodeFormatter(
      TestHelper.buildOptions.copy( namespace = "testNameSpace" ),
      TestHelper.buildServiceDefinition )
    "return empty list with empty lines" in {
      formatter.wrapParameterDict( List( ), "" ) shouldBe empty
    }
    "return unwrapped line" in {
      formatter.wrapParameterDict( List( "A : " + "1" ), "" ).size shouldBe 1
    }
    "return unwrapped line with 79 characters" in {
      formatter.wrapParameterDict( List( "A : " + "a" * 75 ), "" ).size shouldBe 1
    }
    "return unwrapped lines with 2 params" in {
      formatter.wrapParameterDict( List( "A : " + "a" * 74, "B : " + "b" * 74 ), "" ).size shouldBe 2
    }
    "return wrapped line with 80 characters" in {
      formatter.wrapParameterDict( List( "A : " + "a" * 76 ), "" ).size shouldBe 2
    }
    "return first segment of line when wrapping" in {
      formatter.wrapParameterDict( List( "A : " + "a" * 76 ), "" ).iterator.next should be( "A :" )
    }
    "return second segment of line when wrapping" in {
      formatter.wrapParameterDict( List( "A : " + "a" * 76 ), "" ).iterator.drop( 1 ).next should be( "    " + "a" * 76 )
    }
    "return all segments of line when wrapping" in {
      val lines = formatter.wrapParameterDict( List( "A : " + "a" * 75, "B : " + "b" * 75,  "C : " + "c" * 75, "D : " + "d" * 75 ), "" )
      lines.size should be( 4 )
      val it = lines.iterator
      it.next should be( "A : " + "a" * 75 )
      it.next should be( "B : " + "b" * 75 )
      it.next should be( "C : " + "c" * 75 )
      it.next should be( "D : " + "d" * 75 )
    }
    "return all segments of line when wrapping with edge-case" in {
      val lines = formatter.wrapParameterDict( List( "A : " + "a" * 76, "B : " + "b" * 76, "C : " + "c" * 76, "D : " + "d" * 76 ), "" )
      //lines.size should be( 8 )
      val it = lines.iterator
      it.next should be( "A :")
      it.next should be( "    " + "a" * 76 )
      it.next should be( "B :" )
      it.next should be( "    " + "b" * 76 )
      it.next should be( "C :" )
      it.next should be( "    " + "c" * 76 )
      it.next should be( "D :")
      it.next should be( "    " + "d" * 76 )
    }

  }
  "wrapLines" should {
    val formatter = new PythonCodeFormatter(
      TestHelper.buildOptions.copy( namespace = "testNameSpace" ),
      TestHelper.buildServiceDefinition )
    "return empty list with empty lines" in {
      formatter.wrapLines( List( ), "" ) shouldBe empty
    }
    "return unwrapped line" in {
      formatter.wrapLines( List( "1" ), "" ).size shouldBe (1)
    }
    "return unwrapped line with 79 characters and space" in {
      formatter.wrapLines( List( "1" * 70 + " " + "2" * 8 ), "" ).size shouldBe (1)
    }
    "return unwrapped line with 79 characters and no space" in {
      formatter.wrapLines( List( "1" * 79 ), "" ).size shouldBe (1)
    }
    "return unwrapped line with 80 characters and no space" in {
      formatter.wrapLines( List( "1" * 80 ), "" ).size shouldBe (1)
    }
    "return wrapped lines with 79 characters and space" in {
      formatter.wrapLines( List( "1" * 70 + " " + "2" * 8 ), "" ).size shouldBe (1)
    }
    "return wrapped line with 80 characters and no space" in {
      formatter.wrapLines( List( "1" * 78 + " 2" ), "" ).size shouldBe (2)
    }
    "return first segment of line when wrapping" in {
      formatter.wrapLines( List( "1" * 78 + " 2" ), "" ).iterator.next should be( "1" * 78 +s"\n" )
    }
    "return second segment of line when wrapping" in {
      formatter.wrapLines( List( "1" * 78 + " 2" ), "" ).iterator.drop( 1 ).next should be( "2" )
    }
    "return all segments of line when wrapping" in {
      val lines = formatter.wrapLines( List( "1" * 79 + " " + "2" * 79 + " " + "3" * 79 + " " + "4" * 79 ), "" )
      lines.size should be( 4 )
      val it = lines.iterator
      it.next should be( s"""${"1" * 79}\n""" )
      it.next should be( s"""${"2" * 79}\n""" )
      it.next should be( s"""${"3" * 79}\n""" )
      it.next should be( s"""${"4" * 79}""" )
    }

  }
}
