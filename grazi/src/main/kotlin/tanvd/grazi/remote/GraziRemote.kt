package tanvd.grazi.remote

import com.intellij.openapi.project.Project
import tanvd.grazi.GraziConfig
import tanvd.grazi.GraziPlugin
import tanvd.grazi.language.Lang

object GraziRemote {
    private fun isLibExists(lib: String) = GraziPlugin.installationFolder.resolve("lib/$lib").exists()

    fun isAvailableLocally(lang: Lang) = isLibExists(lang.remote.file)

    /** Downloads [lang] to local storage */
    fun download(lang: Lang, project: Project? = null): Boolean {
        if (isAvailableLocally(lang)) return true

        return LangDownloader.download(lang, project)
    }

    /** Downloads all missing languages to local storage*/
    fun downloadMissing(project: Project?) = GraziConfig.get().missedLanguages.forEach { LangDownloader.download(it, project) }
}
