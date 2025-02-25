package tanvd.grazi.ide.ui.components.rules

import org.languagetool.rules.Category
import org.languagetool.rules.Rule
import tanvd.grazi.GraziConfig
import tanvd.grazi.language.Lang
import tanvd.grazi.language.LangTool
import java.util.*

data class RuleWithLang(val rule: Rule, val lang: Lang, val enabled: Boolean, var enabledInTree: Boolean) : Comparable<RuleWithLang> {
    val category: Category = rule.category

    override fun compareTo(other: RuleWithLang) = rule.description.compareTo(other.rule.description)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RuleWithLang

        if (lang != other.lang) return false
        if (rule.description != other.rule.description) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rule.description.hashCode()
        result = 31 * result + lang.hashCode()
        return result
    }
}

data class ComparableCategory(val category: Category) : Comparable<ComparableCategory> {
    val name: String = category.name

    override fun compareTo(other: ComparableCategory) = name.compareTo(other.name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComparableCategory

        if (category.name != other.category.name) return false

        return true
    }

    override fun hashCode(): Int {
        return category.name.hashCode()
    }
}

typealias RulesMap = Map<Lang, Map<ComparableCategory, SortedSet<RuleWithLang>>>

fun LangTool.allRulesWithLangs(langs: Collection<Lang>): RulesMap {
    val state = GraziConfig.get()

    val result = TreeMap<Lang, SortedMap<ComparableCategory, SortedSet<RuleWithLang>>>(Comparator.comparing(Lang::displayName))
    langs.filter { it.jLanguage != null }.forEach { lang ->
        val categories = TreeMap<ComparableCategory, SortedSet<RuleWithLang>>()

        with(getTool(lang)) {
            val activeRules = allActiveRules.toSet()

            fun Rule.isActive() = (id in state.userEnabledRules && id !in state.userDisabledRules)
                || (id !in state.userDisabledRules && id !in state.userEnabledRules && this in activeRules)

            allRules.distinctBy { it.id }.forEach {
                if (!it.isDictionaryBasedSpellingRule) {
                    categories.getOrPut(ComparableCategory(it.category), ::TreeSet).add(RuleWithLang(it, lang, enabled = it.isActive(), enabledInTree = it.isActive()))
                }
            }

            if (categories.isNotEmpty()) result[lang] = categories
        }
    }

    return result
}
