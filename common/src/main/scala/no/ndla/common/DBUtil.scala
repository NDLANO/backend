/*
 * Part of NDLA common.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import scalikejdbc._

object DBUtil {

  /** Builds a where clause from a list of conditions. If the list is empty, an empty SQLSyntax object with no where
    * clause is returned.
    *
    * @param conditions
    *   A list of conditions to be joined with AND.
    * @return
    *   A SQLSyntax object representing the where clause.
    */
  def buildWhereClause(conditions: Seq[SQLSyntax]): SQLSyntax = if (conditions.nonEmpty) {
    val cc = conditions.foldLeft((true, sqls"where ")) { case (acc, cur) =>
      (
        false,
        if (acc._1) sqls"${acc._2} $cur" else sqls"${acc._2} and $cur"
      )
    }
    cc._2
  } else sqls""
}
