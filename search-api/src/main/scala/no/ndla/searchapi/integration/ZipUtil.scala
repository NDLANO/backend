/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.integration

import java.io.*
import java.util.zip.ZipInputStream
import scala.util.Try

object ZipUtil {
  def unzip(zipFile: File, targetDir: File, deleteArchive: Boolean): Try[File] = Try {
    val fis = new FileInputStream(zipFile)
    val zis = new ZipInputStream(fis)
    LazyList.continually(zis.getNextEntry).takeWhile(_ != null).foreach { file =>
      val outFile = new File(targetDir, file.getName)
      outFile.getParentFile.mkdirs()
      val fout   = new FileOutputStream(outFile)
      val buffer = new Array[Byte](1024)
      LazyList
        .continually(zis.read(buffer))
        .takeWhile(_ != -1)
        .foreach(fout.write(buffer, 0, _))
    }

    if (deleteArchive) zipFile.delete()

    targetDir
  }
}
