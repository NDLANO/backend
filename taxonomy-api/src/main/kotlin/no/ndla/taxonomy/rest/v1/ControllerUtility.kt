/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import java.net.URI
import no.ndla.taxonomy.domain.DomainEntity
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException
import no.ndla.taxonomy.service.URNValidator
import no.ndla.taxonomy.service.UpdatableDto
import org.springframework.web.bind.annotation.RequestMapping

private val urnValidator = URNValidator()

fun <T : DomainEntity> validateAndAssignId(entity: T, command: UpdatableDto<T>) {
  command.id.ifPresent { id ->
    urnValidator.validate(id, entity)
    entity.publicId = id
  }
}

fun validateUrn(id: URI, entity: DomainEntity) {
  urnValidator.validate(id, entity)
}

fun handleDuplicateId(command: UpdatableDto<*>): Nothing {
  command.id.ifPresent { throw DuplicateIdException(it.toString()) }
  throw DuplicateIdException()
}

fun controllerLocation(controllerClass: Class<*>): String =
    controllerClass.getAnnotation(RequestMapping::class.java).path[0]
