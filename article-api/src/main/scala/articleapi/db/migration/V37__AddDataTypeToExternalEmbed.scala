/*
 * Part of NDLA article-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package articleapi.db.migration

class V37__AddDataTypeToExternalEmbed extends V35__AddDataTypeToIframeEmbed {
  override val resource = "external"
}
