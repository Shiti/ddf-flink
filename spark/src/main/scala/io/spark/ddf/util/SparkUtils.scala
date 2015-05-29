package io.spark.ddf.util

import java.io.CharArrayWriter
import java.util
import java.util.{Map => JMap}
import com.fasterxml.jackson.core.{JsonGenerator, JsonFactory}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.types._

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.{Column => DFColumn}
import io.ddf.content.Schema
import scala.collection.Map
import scala.collection.mutable.ArrayBuffer
import java.util.{List => JList}
import io.ddf.content.Schema.Column
import com.google.common.collect.Lists
import java.util.ArrayList
import scala.util
import io.ddf.exception.DDFException
import scala.collection.JavaConverters._
import scala.util


/**
  */

object SparkUtils {
  /**
   * Create custom sharkContext with adatao's spark.kryo.registrator
   * @param master
   * @param jobName
   * @param sparkHome
   * @param jars
   * @param environment
   * @return
   */
  def createSparkConf(master: String, jobName: String, sparkHome: String, jars: Array[String],
                      environment: JMap[String, String]): SparkConf = {
    //val conf = SharkContext.createSparkConf(master, jobName, sparkHome, jars, environment.asScala)
    val conf = new SparkConf()
      .setMaster(master)
      .setAppName(jobName)
      .setJars(jars)
      .setExecutorEnv(environment.asScala.toSeq)
    conf.set("spark.kryo.registrator", System.getProperty("spark.kryo.registrator", "io.spark.content.KryoRegistrator"))
  }

  def createSparkContext(master: String, jobName: String, sparkHome: String, jars: Array[String],
                         environment: JMap[String, String]): SparkContext = {
    val conf = createSparkConf(master, jobName, sparkHome, jars, environment)
    new SparkContext(conf)
  }

  def schemaFromDataFrame(schemaRDD: DataFrame): Schema = {
    val schema = schemaRDD.schema
    //println("<<<< schema: " + schema)
    val cols: ArrayList[Column] = Lists.newArrayList();
    for(field <- schema.fields) {
      val colType = spark2DDFType(field.dataType.typeName)
      val colName = field.name
      cols.add(new Column(colName, colType))
    }
    new Schema(null, cols)
  }

  /**
   *
   * @param df the input dataframe
   * @param colNames subset of column that user wants to flatten
   * @return a list of names of non-struct fields flattened from the dataframe
   */
  def flattenColumnNamesFromDataFrame(df: DataFrame, colNames: Array[String]): Array[String] = {
    val result: ArrayBuffer[String] = new ArrayBuffer[String]()
    val schema = df.schema
    val fields =
      if(colNames == null || colNames.isEmpty) {
        schema.fields
      } else {
        val flds:ArrayBuffer[StructField] = new ArrayBuffer[StructField]()
        for(name <- colNames) {
          if (schema.fieldNames.contains(name))
            flds.append(schema.apply(name))
          else
            throw new DDFException("Error: column-name " + name + " does not exist in the dataset")
        }
        flds.toArray
      }

    for(field <- fields) {
      result.appendAll(flattenColumnNamesFromStruct(field))
    }
    result.toArray[String]
  }

  def flattenColumnNamesFromDataFrame(df: DataFrame): Array[String] = {
    flattenColumnNamesFromDataFrame(df, null)
  }

  /**
   * @param structField
   * @return all primitive column paths inside the struct
   */
  private def flattenColumnNamesFromStruct(structField: StructField): Array[String] = {
    var result:ArrayBuffer[String] = new ArrayBuffer[String]()
    flattenColumnNamesFromStruct(structField, result, "")
    result.toArray[String]
  }

  private def flattenColumnNamesFromStruct(structField: StructField, resultList: ArrayBuffer[String], curColName: String): Unit = {
    val colName = if(curColName == "") structField.name else (curColName + "." + structField.name)
    val dType = structField.dataType

    if(dType.typeName != "struct") {
      resultList.append(colName)
    } else {
      val fields = dType.asInstanceOf[StructType].fields
      for(field <- fields) {
        flattenColumnNamesFromStruct(field, resultList, colName)
      }
    }

  }

  /**
   *
   * @param df
   * @return an Array of string showing the dataframe with complex column-object replaced by json string
   */
  def jsonForComplexType(df: DataFrame, sep: String): Array[String] = {
    val schema = df.schema
    val df1: RDD[String] = df.map(r => rowToJSON(schema, r, sep))
    df1.collect()
  }

  private def rowToJSON(rowSchema: StructType, row: Row, separator: String): String = {
    val writer = new CharArrayWriter()
    val gen = new JsonFactory().createGenerator(writer).setRootValueSeparator(null)

    def valWriter: (DataType, Any) => Unit = {
      case (_, null) | (NullType, _)  => gen.writeNull()
      case (StringType, v: String) => gen.writeString(v)
      case (TimestampType, v: java.sql.Timestamp) => gen.writeString(v.toString)
      case (IntegerType, v: Int) => gen.writeNumber(v)
      case (ShortType, v: Short) => gen.writeNumber(v)
      case (FloatType, v: Float) => gen.writeNumber(v)
      case (DoubleType, v: Double) => gen.writeNumber(v)
      case (LongType, v: Long) => gen.writeNumber(v)
      case (DecimalType(), v: java.math.BigDecimal) => gen.writeNumber(v)
      case (ByteType, v: Byte) => gen.writeNumber(v.toInt)
      case (BinaryType, v: Array[Byte]) => gen.writeBinary(v)
      case (BooleanType, v: Boolean) => gen.writeBoolean(v)
      case (DateType, v) => gen.writeString(v.toString)
      case (udt: UserDefinedType[_], v) => valWriter(udt.sqlType, v)

      case (ArrayType(ty, _), v: Seq[_] ) =>
        gen.writeStartArray()
        v.foreach(valWriter(ty,_))
        gen.writeEndArray()

      case (MapType(kv,vv, _), v: Map[_,_]) =>
        gen.writeStartObject()
        v.foreach { p =>
          gen.writeFieldName(p._1.toString)
          valWriter(vv,p._2)
        }
        gen.writeEndObject()

      case (StructType(ty), v: Row) =>
        gen.writeStartObject()
        ty.zip(v.toSeq).foreach {
          case (_, null) =>
          case (field, v) =>
            gen.writeFieldName(field.name)
            valWriter(field.dataType, v)
        }
        gen.writeEndObject()
    }

    var i = 0
    rowSchema.zip(row.toSeq).foreach {
      case (_, null) =>
        if(i > 0)
          gen.writeRaw(separator)
        i = i+1
        gen.writeNull()
      case (field, v) =>
        if(i > 0)
          gen.writeRaw(separator)
        i = i+1
        if(field.dataType.isPrimitive)
          gen.writeRaw(v.toString)
        else
          valWriter(field.dataType, v)
    }
    gen.close()
    writer.toString
  }

  def getDataFrameWithValidColnames(df: DataFrame): DataFrame = {
    // remove '_' if '_' is at the start of a col name
    val colNames = df.columns.map { colName =>
      if (colName.charAt(0) == '_') new DFColumn(colName).as(colName.substring(1)) else new DFColumn(colName)
    }
    df.select(colNames :_*)
  }

  def spark2DDFType(colType: String): String = {
    //println(colType)
    colType match {
      case "integer" => "INT"
      case "string" => "STRING"
      case "float"  => "FLOAT"
      case "double" => "DOUBLE"
      case "timestamp" => "TIMESTAMP"
      case "long"     => "LONG"
      case "boolean"  => "BOOLEAN"
      case "struct" => "STRUCT"
      case "array" => "ARRAY"
      case "map" => "MAP"
      case x => throw new DDFException(s"Type not support $x")
    }
  }
}
