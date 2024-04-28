plugins {
    //id("java-library")
    alias(libs.plugins.mxs.template.jvm.library)
    id("com.android.lint")
}

lint {
    htmlReport = true
    htmlOutput = file("lint-report.html")
    textReport = true
    absolutePaths = false
    ignoreTestSources = true
}

dependencies {
    compileOnly(libs.lint.api)
}