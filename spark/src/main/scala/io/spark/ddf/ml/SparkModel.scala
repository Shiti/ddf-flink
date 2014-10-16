package io.spark.ddf.ml

import io.ddf.ml.{MLClassMethods, Model}
import org.apache.spark.mllib.linalg.{Vectors, Vector}
import io.ddf.ml.MLClassMethods.PredictMethod
import io.ddf.exception.DDFException

/**
  */
class SparkModel(rawModel: Object) extends Model(rawModel) {
  override def predict(point: Array[Double]): Double = {

    val predictMethod = new PredictMethod(this.getRawModel, MLClassMethods.DEFAULT_PREDICT_METHOD_NAME
      , Array(classOf[Vector]))

    if (predictMethod.getMethod == null) {
      throw new DDFException((String.format("Cannot locate method specified by %s", MLClassMethods.DEFAULT_PREDICT_METHOD_NAME)))
    }

    val prediction = predictMethod.instanceInvoke(Vectors.dense(point));
    if (prediction isInstanceOf Double) {
      prediction.asInstanceOf[Double]
    } else if (prediction isInstanceOf Int) {
      (prediction.asInstanceOf[Int]).toDouble
    } else {
      throw new DDFException(String.format("Error getting prediction from model %s", this.getRawModel.getClass.getName))
    }
  }
}
