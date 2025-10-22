/*
 * Part of NDLA network
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients

import no.ndla.network.UnitSuite

class FeideApiClientTest extends UnitSuite {

  test("testFeideExtendedUserInfo roles") {
    val user = FeideExtendedUserInfo("", Seq.empty, None, "", None)
    assert(!user.isTeacher)

    val member = user.copy(eduPersonAffiliation = Seq("member"))
    assert(!member.isTeacher)

    val student = user.copy(eduPersonAffiliation = Seq("member", "student"))
    assert(!student.isTeacher)

    val studentWithPrimary =
      user.copy(eduPersonAffiliation = Seq("member", "student"), eduPersonPrimaryAffiliation = Some("student"))
    assert(!studentWithPrimary.isTeacher)

    val staff = user.copy(eduPersonAffiliation = Seq("member", "staff"))
    assert(staff.isTeacher)

    val faculty = user.copy(eduPersonAffiliation = Seq("member", "faculty"))
    assert(faculty.isTeacher)

    val employee = user.copy(eduPersonAffiliation = Seq("member", "employee"))
    assert(employee.isTeacher)

    val employeeWithStudent = user.copy(eduPersonAffiliation = Seq("member", "employee", "student"))
    assert(employeeWithStudent.isTeacher)

    val studentWithEmployee = user.copy(
      eduPersonAffiliation = Seq("member", "student", "employee"),
      eduPersonPrimaryAffiliation = Some("student"),
    )
    assert(!studentWithEmployee.isTeacher)
  }
}
