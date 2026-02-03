/*
 * Part of NDLA common
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.aws

import no.ndla.testbase.UnitTestSuiteBase

class NdlaEmailClientIntegrationTest extends UnitTestSuiteBase {
  test("Send email via AWS SES (manual)") {
    val areWeTesting = Option(System.getenv("NDLA_TEST_AWS_SES")).contains("true")
    val to           = Option(System.getenv("NDLA_TEST_AWS_SES_EMAIL"))
    assume(areWeTesting && to.isDefined)

    val client = new NdlaEmailClient(
      senderEmail = "noreply@mail.test.ndla.no",
      senderName = "NDLA-testing",
      region = Some("eu-west-1"),
    )

    val subject = "NDLA SES test email"
    val body    = "This is a test email sent by NdlaEmailClient via AWS SES."
    val result  = client.sendEmail(to.get, subject, body)
    result.failIfFailure should be(true)
  }
}
