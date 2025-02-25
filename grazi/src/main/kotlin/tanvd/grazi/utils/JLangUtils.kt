package tanvd.grazi.utils

import kotlinx.html.FlowOrPhrasingContent
import kotlinx.html.strong
import org.languagetool.language.Language
import org.languagetool.language.Languages
import org.languagetool.rules.*
import tanvd.grazi.GraziPlugin
import tanvd.grazi.grammar.Typo
import tanvd.grazi.language.Lang

val Typo.isSpellingTypo: Boolean
    get() = info.rule.isDictionaryBasedSpellingRule

val RuleMatch.typoCategory: Typo.Category
    get() = Typo.Category[rule.category.id.toString()]

val ExampleSentence.text: CharSequence
    get() = example

fun Rule.toDescriptionSanitized() = this.description.replace("**", "")

private fun FlowOrPhrasingContent.toHtml(example: IncorrectExample, mistakeHandler: FlowOrPhrasingContent.(String) -> Unit) {
    Regex("(.*?)<marker>(.*?)</marker>|(.*)").findAll(example.example).forEach {
        val (prefix, mistake, suffix) = it.destructured

        +prefix
        mistakeHandler(mistake)
        +suffix
    }
}

fun FlowOrPhrasingContent.toIncorrectHtml(example: IncorrectExample) {
    toHtml(example) { mistake ->
        if (mistake.isNotEmpty()) {
            strong {
                +mistake.trim()
            }
        }
    }
}

fun FlowOrPhrasingContent.toCorrectHtml(example: IncorrectExample) {
    toHtml(example) { mistake ->
        if (mistake.isNotEmpty() && example.corrections.isNotEmpty()) {
            strong {
                +example.corrections.first().trim()
            }
        }
    }
}

object LangToolInstrumentation {
    fun registerLanguage(lang: Lang) {
        lang.remote.langsClasses.forEach { className ->
            val qualifiedName = "org.languagetool.language.$className"
            if (Languages.get().all { it::class.java.canonicalName != qualifiedName }) {
                Languages.add(GraziPlugin.loadClass(qualifiedName)!!.newInstance() as Language)
            }
        }
    }
}
