/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.net.URI;

/*
 Auto converts URI data types used in domain objects to/from string type used in database
*/
@SuppressWarnings("unused")
@Converter(autoApply = true)
public class UriTypeConverter implements AttributeConverter<URI, String> {
    @Override
    public String convertToDatabaseColumn(URI uri) {
        return uri != null ? uri.toString() : null;
    }

    @Override
    public URI convertToEntityAttribute(String s) {
        return s != null ? URI.create(s) : null;
    }
}
