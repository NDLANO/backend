/*
 * Part of NDLA article-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package articleapi.db.migration

class V37__AddDataTypeToExternalEmbed extends V35__AddDataTypeToIframeEmbed {
  override val resource = "external"
}
