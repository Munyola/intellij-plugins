package tanvd.grazi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPlainText
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import tanvd.grazi.ide.GraziInspection
import tanvd.grazi.ide.msg.GraziStateLifecycle
import tanvd.grazi.language.Lang
import tanvd.grazi.utils.filterFor
import java.io.File

abstract class GraziTestBase(private val withSpellcheck: Boolean) : LightJavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return File("src/test/resources").canonicalPath
    }

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(*inspectionTools)

        GraziConfig.update { state ->
            // remove dialects
            state.update(enabledLanguages = Lang.values().filter { it !in listOf(Lang.CANADIAN_ENGLISH, Lang.BRITISH_ENGLISH, Lang.AUSTRIAN_GERMAN, Lang.PORTUGAL_PORTUGUESE) }.toSet(), enabledSpellcheck = withSpellcheck)
        }

        while (ApplicationManager.getApplication().messageBus.hasUndeliveredEvents(GraziStateLifecycle.topic)) {
            Thread.sleep(500)
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return object : ProjectDescriptor(LanguageLevel.JDK_1_8) {
            override fun getSdk(): Sdk? {
                return JavaSdk.getInstance().createJdk("jdk8", System.getenv("JAVA_HOME")
                    ?: "/usr/lib/jvm/java-8-openjdk-amd64", false)
            }
        }
    }

    protected fun runHighlightTestForFile(file: String) {
        myFixture.configureByFile(file)

        // Markdown changed PSI during highlighting
        (myFixture as? CodeInsightTestFixtureImpl)?.canChangeDocumentDuringHighlighting(true)

        myFixture.testHighlighting(true, false, false, file)
    }

    fun plain(vararg texts: String) = plain(texts.toList())

    fun plain(texts: List<String>): Collection<PsiElement> {
        return texts.flatMap { myFixture.configureByText("${it.hashCode()}.txt", it).filterFor<PsiPlainText>() }
    }


    companion object {
        private val inspectionTools by lazy { arrayOf(GraziInspection()) }
    }
}
