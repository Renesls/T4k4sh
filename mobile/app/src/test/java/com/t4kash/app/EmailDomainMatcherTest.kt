package com.t4kash.app

import com.t4kash.app.ui.detectUniversityFromEmail
import com.t4kash.app.ui.extractEmailDomain
import com.t4kash.app.ui.model.UniversityDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmailDomainMatcherTest {
    private val universities = listOf(
        UniversityDto(1, "Universidad Americana", listOf("uamv.edu.ni")),
        UniversityDto(2, "Universidad Nacional", listOf("unan.edu.ni"))
    )

    @Test
    fun detectsUniversityIgnoringEmailCaseAndSpaces() {
        val university = detectUniversityFromEmail(
            "  Alumno@UAMV.EDU.NI ",
            universities
        )

        assertEquals(1, university?.idUniversidad)
    }

    @Test
    fun returnsNullForPersonalOrIncompleteEmail() {
        assertNull(detectUniversityFromEmail("persona@gmail.com", universities))
        assertNull(detectUniversityFromEmail("persona@", universities))
    }

    @Test
    fun doesNotAcceptDomainsThatOnlyLookSimilar() {
        assertNull(
            detectUniversityFromEmail(
                "persona@uamv.edu.ni.falso.com",
                universities
            )
        )
        assertEquals("uamv.edu.ni", extractEmailDomain("persona@uamv.edu.ni"))
    }
}
