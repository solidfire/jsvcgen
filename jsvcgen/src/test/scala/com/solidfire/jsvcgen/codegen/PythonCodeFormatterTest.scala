package com.solidfire.jsvcgen.codegen

import com.solidfire.jsvcgen.model.{Member, TypeDefinition, TypeUse}
import org.scalatest.{Matchers, WordSpec}


class PythonCodeFormatterTest extends WordSpec with Matchers {

  "getTypeImports" should {
    "return one import statements" in {
      val typeDefinition = TypeDefinition("test", None,
      List(Member("SomeNumber", TypeUse("integer")),
        Member("CustomTypeField1", TypeUse("CustomType")),
        Member("CustomTypeField2", TypeUse("CustomType"))
      )
      )
      val formatter = new PythonCodeFormatter(
        TestHelper.buildOptions.copy(namespace = "testNameSpace"),
        TestHelper.buildServiceDefinition)
      val imports = formatter.getTypeImports(typeDefinition, TestHelper.buildOptions)
      imports should be ("\nfrom com.example.CustomType import CustomType")
    }
  }

  "isDirectType" should {
    "return true for direct types" in {
      val member = Member("SomeNumber", TypeUse("integer"))
      val formatter = new PythonCodeFormatter(
        TestHelper.buildOptions.copy(namespace = "testNameSpace"),
        TestHelper.buildServiceDefinition)
      formatter.isDirectType(member) should be (true)
    }
    "directTypes gets integer" in {
      val formatter = new PythonCodeFormatter(
        TestHelper.buildOptions.copy(namespace = "testNameSpace"),
        TestHelper.buildServiceDefinition)
      formatter.directTypeNames.get("integer").isDefined should be (true)
    }
  }

}
