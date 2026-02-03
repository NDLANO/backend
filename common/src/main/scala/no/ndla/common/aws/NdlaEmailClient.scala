/*
 * Part of NDLA common
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.aws

import no.ndla.common.TryUtil.throwIfInterrupted
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sesv2.{SesV2Client, SesV2ClientBuilder}
import software.amazon.awssdk.services.sesv2.model.*

import scala.util.Try

class NdlaEmailClient(senderEmail: String, senderName: String, region: Option[String]) {
  lazy val client: SesV2Client = {
    val builder: SesV2ClientBuilder = SesV2Client.builder()
    region match {
      case Some(value) => builder.region(Region.of(value)).build()
      case None        => builder.build()
    }
  }

  def sendEmail(to: String, subject: String, bodyStr: String): Try[Boolean] = {
    Try.throwIfInterrupted {
      val destination      = Destination.builder().toAddresses(to).build()
      val content          = Content.builder().data(bodyStr).build()
      val sub              = Content.builder().data(subject).build()
      val body             = Body.builder().html(content).build()
      val msg              = Message.builder().subject(sub).body(body).build()
      val bodyEmailContent = EmailContent.builder().simple(msg).build()
      val emailRequest     = SendEmailRequest
        .builder()
        .destination(destination)
        .fromEmailAddress(s"$senderName <$senderEmail>")
        .content(bodyEmailContent)
        .build()

      val response = client.sendEmail(emailRequest)

      Option(response.messageId()).exists(_.nonEmpty)
    }
  }

}
