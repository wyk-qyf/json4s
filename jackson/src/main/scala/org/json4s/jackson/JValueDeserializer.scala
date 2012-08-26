package org.json4s
package jackson

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.{DeserializationFeature, DeserializationContext, JsonDeserializer}
import com.fasterxml.jackson.core.{JsonToken, JsonParser}
import org.json4s._
import scala._
import collection.mutable.ArrayBuffer

class JValueDeserializer(factory: TypeFactory, klass: Class[_]) extends JsonDeserializer[Object] {
  def deserialize(jp: JsonParser, ctxt: DeserializationContext): Object = {
    if (jp.getCurrentToken == null) {
      jp.nextToken()
    }

    val value = jp.getCurrentToken match {
      case JsonToken.VALUE_NULL => JNull
      case JsonToken.VALUE_NUMBER_INT => JInt(BigInt(jp.getText))
      case JsonToken.VALUE_NUMBER_FLOAT =>
        if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) JDecimal(jp.getDecimalValue)
        else JDouble(jp.getDoubleValue)
      case JsonToken.VALUE_STRING => JString(jp.getText)
      case JsonToken.VALUE_TRUE => JBool(true)
      case JsonToken.VALUE_FALSE => JBool(false)
      case JsonToken.START_ARRAY => {
        val values = new ArrayBuffer[JValue]()
        while(jp.getCurrentToken != JsonToken.END_ARRAY) {
          val name = jp.getCurrentName
          jp.nextToken()
          if (jp.getCurrentToken != JsonToken.END_ARRAY) {
            values += jp.getCodec.readValue(jp, Types.build(factory, manifest[JValue]))
          }
        }
        JArray(values.toList)
      }
      case JsonToken.START_OBJECT => {
        jp.nextToken()
        deserialize(jp, ctxt)
      }
      case JsonToken.FIELD_NAME | JsonToken.END_OBJECT => {
        val fields = new ArrayBuffer[JField]
        while (jp.getCurrentToken != JsonToken.END_OBJECT) {
          val name = jp.getCurrentName
          jp.nextToken()
          fields += JField(name, jp.getCodec.readValue(jp, Types.build(factory, manifest[JValue])))
          jp.nextToken()
        }
        JObject(fields.toList)
      }
      case _ => throw ctxt.mappingException(classOf[JValue])
    }

    if (!klass.isAssignableFrom(value.getClass)) {
      throw ctxt.mappingException(klass)
    }

    value
  }

  override def isCachable = true
}
