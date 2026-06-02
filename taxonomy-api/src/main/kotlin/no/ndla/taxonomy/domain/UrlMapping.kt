/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.net.URI

@Entity
@Table(name = "url_map")
class UrlMapping(
    @Id @Column var oldUrl: String,
    @Column(name = "public_id") var publicId: URI,
    @Column(name = "subject_id") var subjectId: URI?,
)
