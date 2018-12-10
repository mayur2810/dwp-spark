package com.sope.etl

import com.sope.etl.yaml.YamlFile.End2EndYaml
import com.sope.utils.Logging
import org.apache.commons.cli.{BasicParser, Options}
import org.apache.spark.sql.SparkSession

/**
  * Helper for Running YAML Transformation
  *
  * @author mbadgujar
  */
object YamlRunner extends Logging {

  private val MainYamlFileOpt = "main_yaml_file"
  private val options = new Options().addOption(MainYamlFileOpt, true, "Main yaml file name")

  def main(args: Array[String]): Unit = {
    val cmdLine = new BasicParser().parse(options, args, true)
    val optionMap = cmdLine.getOptions.map(option => option.getOpt.trim -> option.getValue.trim).toMap
    val mainYamlFile =  optionMap(MainYamlFileOpt)
    logInfo(s"Parsing Main Yaml File: $mainYamlFile")
    val end2endYaml = End2EndYaml(mainYamlFile)
    logInfo("Successfully parsed YAML File")
    logInfo("Initializing Spark context & executing the flow..")
    val session = SparkSession.builder()
      .enableHiveSupport()
      .getOrCreate()
    end2endYaml.performTransformations(session.sqlContext)
  }

}