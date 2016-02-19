package com.solidfire.jsvcgen.codegen

import com.solidfire.jsvcgen.model.{Member, TypeDefinition, TypeUse}
import org.scalatest.{Matchers, WordSpec}

import scala.util.Random


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
      val imports = formatter.getTypeImports( List( typeDefinition ) )
      imports should endWith( "import CustomType" )
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
      val lines = formatter.wrapParameterDict( List( "A : " + "a" * 75, "B : " + "b" * 75, "C : " + "c" * 75, "D : " + "d" * 75 ), "" )
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
      it.next should be( "A :" )
      it.next should be( "    " + "a" * 76 )
      it.next should be( "B :" )
      it.next should be( "    " + "b" * 76 )
      it.next should be( "C :" )
      it.next should be( "    " + "c" * 76 )
      it.next should be( "D :" )
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
      formatter.wrapLines( List( "1" ), "" ).size shouldBe 1
    }
    "return unwrapped line with 79 characters and space" in {
      formatter.wrapLines( List( "1" * 70 + " " + "2" * 8 ), "" ).size shouldBe 1
    }
    "return unwrapped line with 79 characters and no space" in {
      formatter.wrapLines( List( "1" * 79 ), "" ).size shouldBe 1
    }
    "return unwrapped line with 80 characters and no space" in {
      formatter.wrapLines( List( "1" * 80 ), "" ).size shouldBe 1
    }
    "return wrapped lines with 79 characters and space" in {
      formatter.wrapLines( List( "1" * 70 + " " + "2" * 8 ), "" ).size shouldBe 1
    }
    "return wrapped line with 80 characters and no space" in {
      formatter.wrapLines( List( "1" * 78 + " 2" ), "" ).size shouldBe 2
    }
    "return first segment of line when wrapping" in {
      formatter.wrapLines( List( "1" * 78 + " 2" ), "" ).iterator.next should be( "1" * 78 + s"\n" )
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

  "ordered" should {
    val formatter = new PythonCodeFormatter(
      TestHelper.buildOptions.copy( namespace = "testNameSpace" ),
      TestHelper.buildServiceDefinition )
    val mA = Member("a", TypeUse(typeName = "A"))
    val mB = Member("b", TypeUse(typeName = "B"))
    val mC = Member("c", TypeUse(typeName = "C"))
    val mD = Member("d", TypeUse(typeName = "D"))
    val mE = Member("e", TypeUse(typeName = "E"))
    val mF = Member("f", TypeUse(typeName = "F"))
    val mG = Member("g", TypeUse(typeName = "G"))
    val mH = Member("h", TypeUse(typeName = "H"))
    val tA = TypeDefinition(name="A", members = List(mD))
    val tB = TypeDefinition(name="B", members = List(mC))
    val tC = TypeDefinition(name="C", members = List())
    val tD = TypeDefinition(name="D", members = List())
    val tE = TypeDefinition(name="E", members = List(mD))
    val tF = TypeDefinition(name="F", members = List(mE))
    val tG = TypeDefinition(name="G", members = List(mF))
    val tH = TypeDefinition(name="H", members = List(mG))
    val tI = TypeDefinition(name="I", members = List(mA, mB, mE, mF))
    "keep same order with no members" in {
      formatter.ordered(List(tA, tB)) shouldBe List(tA, tB)
    }
    "sort order by name with no members" in {
      formatter.ordered(List(tB, tA)) shouldBe List(tA, tB)
    }
    "keep same order with ascending dependency" in {
      formatter.ordered(List(tD, tA)) shouldBe List(tD, tA)
    }
    "switch order with ascending dependency" in {
      formatter.ordered(List(tA, tD)) shouldBe List(tD, tA)
    }
    "keep same order with 3 TD with one dependency" in {
      formatter.ordered(List(tA, tB, tC)) shouldBe List(tC, tA, tB)
    }
    "sort order with 3 TD with one dependency" in {
      formatter.ordered(List(tB, tA, tC)) shouldBe List(tC, tA, tB)
    }
    "reverse order with 3 TD with one dependency" in {
      formatter.ordered(List(tC, tA, tB)) shouldBe List(tC, tA, tB)
    }
    "switch order with 3 TD with one dependency" in {
      formatter.ordered(List(tB, tC, tA)) shouldBe List(tC, tA, tB)
    }
    "keep same order with ascending dependency large set" in {
      formatter.ordered(List(tC, tD, tA, tB)) shouldBe List(tC, tD, tA, tB)
    }
    "switch order with ascending dependency large set" in {
      formatter.ordered(List(tA, tB, tC, tD)) shouldBe List(tC, tD, tA, tB)
    }
    "sort order by name and ascending dependency large set" in {
      formatter.ordered(List(tD, tC, tB, tA)) shouldBe List(tC, tD, tA, tB)
    }
    "sort all members from accending" in {
      formatter.ordered(List(tA, tB, tC, tD, tE, tF, tG, tH)) shouldBe List(tC, tD, tA, tB, tE, tF, tG, tH)
    }
    "sort all members from decending" in {
      formatter.ordered(List(tH, tG, tF, tE, tD, tC, tB, tA)) shouldBe List(tC, tD, tA, tB, tE, tF, tG, tH)
    }
    "sort all members from random order" in {
      formatter.ordered(Random.shuffle(List(tA, tB, tC, tD, tE, tF, tG, tH))) shouldBe List(tC, tD, tA, tB, tE, tF, tG, tH)
    }
    "multi dependency is last" in {
      formatter.ordered(List(tI, tH, tG, tF, tE, tD, tC, tB, tA)).last shouldBe tI
    }
    "multi dependency from random order is last" in {
      formatter.ordered(Random.shuffle(List(tI, tH, tG, tF, tE, tD, tC, tB, tA))).last shouldBe tI
    }
  }
}
