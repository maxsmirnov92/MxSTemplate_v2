import org.gradle.kotlin.dsl.api
import org.gradle.kotlin.dsl.libs

plugins {
    alias(libs.plugins.mxs.template.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api("core_utils.libs:commonutils-jre-1.0@jar")

    implementation(libs.decoro)
    implementation(libs.picasso)
    implementation(libs.jodaTime)

    implementation(libs.kotlinx.serialization.json)
}