package no.ndla.conceptapi.model.api

import com.scalatsi.TypescriptType.TSNull
import com.scalatsi._

object TSTypes {
  // This alias is required since scala-tsi doesn't understand that Null is `null`
  // See: https://github.com/scala-tsi/scala-tsi/issues/172
  implicit val nullTsType: TSType[Null] = TSType(TSNull)
}
