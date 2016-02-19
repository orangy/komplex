
package komplex.tests.model

import komplex.model.Scenario
import komplex.model.Scenarios
import komplex.utils.BuildDiagnostic
import komplex.utils.plus
import org.junit.Assert
import org.junit.Test

class modelBaseTests {

    @Test fun buildDiagnostics() {
        Assert.assertEquals(BuildDiagnostic.Success, BuildDiagnostic.Success + BuildDiagnostic.Success)
        Assert.assertEquals(BuildDiagnostic.Fail, BuildDiagnostic.Success + BuildDiagnostic.Fail)
        Assert.assertEquals(BuildDiagnostic.Fail, BuildDiagnostic.Fail + BuildDiagnostic.Success)
        Assert.assertEquals(BuildDiagnostic.Fail, BuildDiagnostic.Fail + BuildDiagnostic.Fail)
        Assert.assertEquals(arrayListOf("1","2"), (BuildDiagnostic.Fail("1") + BuildDiagnostic.Fail("2")).messages)
    }

    @Test fun Scenarios() {
        class S(val name: String) : Scenario {}
        val s1 = S("1")
        val s2 = S("2")
        val ss1 = Scenarios(listOf(s1,s2),"ss1")
        Assert.assertEquals(ss1, ss1.combine(Scenarios.Default_))
        Assert.assertEquals(ss1, ss1.combine(Scenarios.Same))
        Assert.assertEquals(Scenarios.All, ss1.combine(Scenarios.All))
    }
}
