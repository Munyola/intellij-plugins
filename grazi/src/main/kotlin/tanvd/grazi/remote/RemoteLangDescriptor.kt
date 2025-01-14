package tanvd.grazi.remote

import tanvd.grazi.ide.ui.components.dsl.msg

enum class RemoteLangDescriptor(val langsClasses: List<String>, val size: String, shortCode: String) {
    ENGLISH(listOf("BritishEnglish", "AmericanEnglish", "CanadianEnglish", "AustralianEnglish", "NewZealandEnglish", "SouthAfricanEnglish"), "14 MB", "en"),
    RUSSIAN(listOf("Russian"), "3 MB", "ru"),
    PERSIAN(listOf("Persian"), "1 MB", "fa"),
    FRENCH(listOf("French"), "4 MB", "fr"),
    GERMAN(listOf("GermanyGerman", "AustrianGerman", "SwissGerman"), "19 MB", "de"),
    POLISH(listOf("Polish"), "5 MB", "pl"),
    ITALIAN(listOf("Italian"), "1 MB", "it"),
    DUTCH(listOf("Dutch"), "17 MB", "nl"),
    PORTUGUESE(listOf("PortugalPortuguese", "BrazilianPortuguese", "AngolaPortuguese", "MozambiquePortuguese"), "5 MB", "pt"),
    CHINESE(listOf("Chinese"), "3 MB", "zh"),
    GREEK(listOf("Greek"), "1 MB", "el"),
    JAPANESE(listOf("Japanese"), "1 MB", "ja"),
    ROMANIAN(listOf("Romanian"), "1 MB", "ro"),
    SLOVAK(listOf("Slovak"), "3 MB", "sk"),
    SPANISH(listOf("Spanish"), "2 MB", "es"),
    UKRAINIAN(listOf("Ukrainian"), "6 MB", "uk");

    val file: String by lazy { "$shortCode-${msg("grazi.languagetool.version")}.jar" }
    val url: String by lazy { "${msg("grazi.maven.repo.url")}/$file" }
}
