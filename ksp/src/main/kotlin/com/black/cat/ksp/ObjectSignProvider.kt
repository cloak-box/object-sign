package com.black.cat.ksp

import com.black.cat.annotation.ObjectSignField
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.writeTo

class ObjectSignProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return ObjectSignProviderProcessor(environment.codeGenerator, environment.logger)
  }
}

private class ObjectSignProviderProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger
) : SymbolProcessor {
  @OptIn(KspExperimental::class)
  override fun process(resolver: Resolver): List<KSAnnotated> {
    val signClasses = mutableMapOf<String, SignClass>()
    resolver
      .getSymbolsWithAnnotation(ObjectSignField::class.java.name)
      .filter {
        if (it is KSPropertyDeclaration) {
          val objectSignField =
            it.getAnnotationsByType(ObjectSignField::class).firstOrNull() ?: return@filter false
          return@filter !objectSignField.ignore
        }
        return@filter false
      }
      .map { it as KSPropertyDeclaration }
      .forEach {
        val clazz = it.closestClassDeclaration()
        if (clazz?.qualifiedName != null) {
          val className = clazz.qualifiedName!!.asString()

          val signClass =
            signClasses.getOrPut(className) {
              SignClass(it.containingFile!!).apply {
                this.className = className
                this.packageName = clazz.packageName.asString()
                this.simpleName = clazz.simpleName.asString()
              }
            }
          signClass.fieldNames.add(
            it.simpleName.asString() to it.type.resolve().declaration.qualifiedName?.asString()
          )
        }
      }

    signClasses.forEach { (_, signClass) ->
      signClass.fieldNames.sortBy { it.first }
      var signStr = "return \"\""
      signClass.fieldNames.forEach {
        signStr += "+ ${it.first}"
        if (it.second != null && signClasses[it.second] != null) {
          signStr += ".generateSign()"
        }
      }

      val generateSign =
        FunSpec.builder("generateSign")
          .addModifiers(KModifier.INLINE)
          .receiver(ClassName(signClass.packageName, signClass.simpleName))
          .returns(String::class)
          .addStatement(signStr)
          .build()

      FileSpec.builder(signClass.packageName, signClass.simpleName)
        .addFunction(generateSign)
        .build()
        .writeTo(codeGenerator, Dependencies(true, signClass.containingFile))
    }

    return emptyList()
  }
}

private class SignClass(val containingFile: KSFile) {
  var className = ""
  var packageName = ""
  var simpleName = ""
  var fieldNames = mutableListOf<Pair<String, String?>>()
}
