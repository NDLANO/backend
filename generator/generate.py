#!/usr/bin/env python
# coding:utf-8

import os
import csv
import re
from datetime import datetime

codelist_template = """
/*
 * Part of GDL language.
 * Copyright (C) %YEAR% Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

object CodeLists {

  sealed abstract class Iso639 {
    def id: String
    def part2b: Option[String]
    def part2t: Option[String]
    def part1: Option[String]
    def scope: Option[String]
    def languageType: Option[String]
    def refName: String
    def comment: Option[String]
  }

  sealed abstract class Iso15924 {
    def code: String
    def no: Int
    def englishName: String
    def frenchName: String
    def pva: Option[String]
    def date: String
  }

  sealed abstract class Iso3166 {
    def code: String
    def name: String
  }

  case class Iso639Val(id: String, part2b: Option[String], part2t: Option[String], part1: Option[String], scope: Option[String], languageType: Option[String], refName: String, comment: Option[String]) extends Iso639
  case class Iso15924Val(code: String, no: Int, englishName: String, frenchName: String, pva: Option[String], date: String) extends Iso15924
  case class Iso3166Val(code: String, name: String) extends Iso3166

  val iso15924Definitions = Seq(
    %ISO15924%
  )

  val iso3166Definitions = Seq(
    %ISO3166%
  )

  val iso639Definitions: Seq[Iso639Val] = %ISO639%
}
"""

iso_639_part_template = """
/*
 * Part of GDL language.
 * Copyright (C) %YEAR% Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

import io.digitallibrary.language.model.CodeLists.Iso639Val

object Iso639List_%NUM% {
  val items = Seq(
    %CODELIST%
  )
}
"""


comment_pattern = re.compile(r'\s*#.*$')

script_dir = os.path.dirname(os.path.realpath(__file__))
project_dir = os.path.dirname(script_dir)

codelists_file = os.path.join(project_dir, "src/main/scala/io/digitallibrary/language/model/CodeLists.scala")
iso639_part_file = os.path.join(project_dir, "src/main/scala/io/digitallibrary/language/model/Iso639List_%NUM%.scala")

iso_639_definitions_file = os.path.join(script_dir, "iso-639-3_20170202.tab")
iso_3166_definitions_file = os.path.join(script_dir, "iso-3166-2.csv")
iso15924_definitions_file = os.path.join(script_dir, "iso15924-utf8-20170726.txt")


def create_iso3166(template):
    with open(iso_3166_definitions_file, 'r') as f:
        lines = []
        reader = csv.reader(f, delimiter=',')
        next(reader) # skip heading
        for row in reader:
            lines.append('Iso3166Val("{}", "{}")'.format(row[1], row[0]))

    print ">> Adding {} entries for ISO-3166".format(len(lines))
    return template.replace("%ISO3166%", ",\n    ".join(lines))


def create_iso15924(template):
    with open(iso15924_definitions_file, 'r') as f:
        reader = csv.reader(skip_comments(f), delimiter=';')
        lines = []
        for row in reader:
            code = row[0]
            no = int(row[1])
            englishName = row[2]
            frenchName = row[3]
            pva = 'Some("{}")'.format(row[4]) if row[4] else "None"
            date = row[5]
            lines.append('Iso15924Val("{}", {}, "{}", "{}", {}, "{}")'.format(code, no, englishName, frenchName, pva, date))

        print ">> Adding {} entries for ISO-15924".format(len(lines))
        return template.replace("%ISO15924%", ",\n    ".join(lines))


def create_iso639(template):
    def chunks(l, n):
        return [l[i:i + n] for i in xrange(0, len(l), n)]

    def process_row(row):
        id = row[0]
        part_2b = 'Some("{}")'.format(row[1]) if row[1] else "None"
        part_2t = 'Some("{}")'.format(row[2]) if row[2] else "None"
        part_1 = 'Some("{}")'.format(row[3]) if row[3] else "None"
        scope = 'Some("{}")'.format(row[4]) if row[4] else "None"
        language_type = 'Some("{}")'.format(row[5]) if row[5] else "None"
        ref_name = row[6]
        comment = 'Some("{}")'.format(row[7]) if row[7] else "None"
        return 'Iso639Val("{}", {}, {}, {}, {}, {}, "{}", {})'.format(id, part_2b, part_2t, part_1, scope, language_type, ref_name, comment)

    with open(iso_639_definitions_file, 'r') as f:
        reader = csv.reader(f, delimiter='\t')
        lines = map(process_row, reader)

        sublists = chunks(lines, 800)
        for i, sublist in enumerate(sublists):
            create_iso_639_part_file(sublist, i + 1)

        seq = " ++ ".join(["Iso639List_{}.items".format(i+1) for i in range(len(sublists))])

    return template.replace("%ISO639%", seq)


def create_iso_639_part_file(lines, counter):
    defs = ",\n    ".join(lines)
    with_defs = iso_639_part_template.replace("%CODELIST%", defs)
    with_classname = with_defs.replace("%NUM%", str(counter))
    with_year = with_classname.replace("%YEAR%", str(datetime.now().year))

    filename = os.path.join(project_dir, iso639_part_file.replace("%NUM%", str(counter)))

    with open(filename, 'w') as f:
        f.write(with_year)

    print ">> Added {} entries in file {} for ISO-639".format(len(lines), filename)


def skip_comments(lines):
    for line in lines:
        line = re.sub(comment_pattern, '', line).strip()
        if line:
            yield line


if __name__ == '__main__':
    with_iso639 = create_iso639(codelist_template)
    with_iso15924 = create_iso15924(with_iso639)
    with_iso3166 = create_iso3166(with_iso15924)
    with_year = with_iso3166.replace("%YEAR%", str(datetime.now().year))

    with open(codelists_file, 'w') as f:
        f.write(with_year)
