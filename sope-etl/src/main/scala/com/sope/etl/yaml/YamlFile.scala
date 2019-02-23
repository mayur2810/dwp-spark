package com.sope.etl.yaml

import com.fasterxml.jackson.databind.JsonMappingException
import com.sope.etl.yaml.YamlParserUtil._
import com.sope.utils.Logging

import scala.util.{Failure, Success, Try}

/**
  * A wrapper class with utilities around Yaml file
  *
  * @author mbadgujar
  */
abstract class YamlFile[T](yamlPath: String, substitutions: Option[Seq[Any]] = None, modelClass: Class[T])
  extends Logging {

  protected val model: T = serialize

  /*
     Updates Placeholders with values provided for Substitution
   */
  private def updatePlaceHolders(): String = {
    substitutions.get
      .zipWithIndex
      .map { case (value, index) => "$" + (index + 1) -> convertToYaml(value) }
      .foldLeft(readYamlFile(yamlPath)) { case (yamlStr, (key, value)) => yamlStr.replace(key, value) }
  }

  /*
    Gets Parse error message
   */
  private def getParseErrorMessage(errorLine: Int, errorColumn: Int): String = {
    val lines = getText.split("\\R+").zipWithIndex
    val errorLocation = lines.filter(_._2 == errorLine - 1)
    s"Encountered issue while parsing Yaml File : $getYamlFileName. Error Line No. : $errorLine:$errorColumn\n" +
      errorLocation(0)._1 + s"\n${(1 until errorColumn).map(_ => " ").mkString("")}^"
  }


  /**
    * Get Yaml File Name
    *
    * @return [[String]] file name
    */
  def getYamlFileName: String = yamlPath.split("[\\\\/]").last

  /**
    * Get Yaml Text. Substitutes values if they are provided
    *
    * @return [[String]]
    */
  def getText: String = substitutions.fold(readYamlFile(yamlPath))(_ => updatePlaceHolders())

  /**
    * Serialize the YAML to provided Type
    *
    * @return T
    */
  def serialize: T = Try {
    parseYAML(getText, modelClass)
  } match {
    case Success(t) =>
      logInfo("Successfully parsed YAML File")
      logDebug(s"Parsed YAML file :-\n $getText")
      t
    case Failure(e) => e match {
      case e: JsonMappingException =>
        Option(e.getLocation) match {
          case Some(location) =>
            val errorMessage = getParseErrorMessage(location.getLineNr, location.getColumnNr)
            logError(errorMessage + s"\n${e.getMessage}")
          case None =>
        }
        throw e
      case _ => throw e
    }
  }

}
