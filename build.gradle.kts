plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.spotless)
}

val ktlintEditorConfig: Map<String, String> =
    mapOf(
        "indent_size" to "4",
        "continuation_indent_size" to "4",
        "max_line_length" to "off",
        "ktlint_code_style" to "ktlint_official",
        "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
    )

spotless {
    kotlin {
        target("composeApp/src/**/*.kt")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintEditorConfig)
    }
    kotlinGradle {
        target("*.gradle.kts", "composeApp/*.gradle.kts")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintEditorConfig)
    }
}
