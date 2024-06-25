import com.android.build.api.dsl.ApkSigningConfig
//import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.mxs.template.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    //Используем FirebaseCrashlytics до тех пор, пока Huawei это позволяет
//    alias(libs.plugins.gms)
//    alias(libs.plugins.firebase.crashlytics)
//    alias(libs.plugins.firebase.appdistribution)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.mxs.template.room)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.navigation.safeargs.kotlin)
}

object AppConfig {
    private const val defaultBuildNumber = "dev"
    private const val defaultBranchName = "HEAD"

    private val buildNumber = System.getenv("BUILD_NUMBER") ?: defaultBuildNumber

    val versionName = "1.$buildNumber"
    val versionCode = Integer.parseInt(System.getenv("BUILD_NUMBER") ?: "1")
}

if (isGoogleBuild(gradle) == false) {
    System.out.println("Applying plugin: com.huawei.agconnect")

    with(pluginManager) {
        apply("com.huawei.agconnect")
    }
}

/**
 * Проверка всех возможных google-флаворов от 'app'
 * исходя из запущенной таски
 */
fun isGoogleBuild(gradle: Gradle): Boolean? {
    val googleFlavors = arrayOf("appProdGoogle", "appDevGoogle")
    val taskNames = gradle.startParameter.taskNames
    if (taskNames.isEmpty()) {
        return null
    }
    return taskNames.any { task ->
        googleFlavors.any { flavorName ->
            task.contains(flavorName, true)
        }
    }
}

android {
    namespace = "net.maxsmr.mxstemplate"

    defaultConfig {
        applicationId = "net.maxsmr.mxstemplate"
        versionCode = AppConfig.versionCode
        versionName = AppConfig.versionName
        project.ext.set("archivesBaseName", "${project.name}-${AppConfig.versionName}")

        buildConfigField("int", "PROTOCOL_VERSION", "1")
        buildConfigField("String", "AUTHORIZATION_RADAR_IO", "\"prj_live_pk_1e1327e1118b82d07dc7ad93525c6795688e5561\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {

        create("release") {
            val properties = Properties()
            properties.load(FileInputStream(File(rootDir, "app/keystore.properties")))
            storeFile = File("${System.getenv("ANDROID_HOME")}/release.keystore")
            keyAlias = properties.getProperty("alias")
            keyPassword = properties.getProperty("signingPassword")
            storePassword = properties.getProperty("signingPassword")
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            multiDexKeepProguard = file("multidex-config.pro")
            signingConfig = null
        }
        getByName("debugMinified") {
            isDebuggable = true
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            multiDexKeepProguard = file("multidex-config.pro")
//            configure<CrashlyticsExtension> {
//                mappingFileUploadEnabled = true
//            }
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            //signingConfig = signingConfigs.getAt("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            multiDexKeepProguard = file("multidex-config.pro")
//            configure<CrashlyticsExtension> {
//                mappingFileUploadEnabled = true
//            }
//            loadSigningConfigForBuildType(File(rootDir, "app/keystore.properties"), "release", true)
        }
    }

    productFlavors {
        /**
         * дебаг вариант сборки с отличным от релиза package name
         */
        getByName("appDev") {
            applicationIdSuffix = ".debug"

//            firebaseAppDistribution {
//                groups = "App-dev"
//                releaseNotesFile = "app/notes.txt"
//            }

            signingConfig = signingConfigs.getByName("debug")
        }

        /**
         * Релизный вариант сборки
         */
        getByName("appProd") {
//            firebaseAppDistribution {
//                groups = "App"
//                releaseNotesFile = "app/notes.txt"
//            }

            signingConfig = signingConfigs.getByName("release")
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val flavour = variant.flavorName
                val builtType = variant.buildType.name
                val versionName = variant.versionName
                output.outputFileName =
                    "${flavour}${builtType.capitalize()}_${versionName}.apk"
            }
    }

    lint {
        checkReleaseBuilds = false
        // Or, if you prefer, you can continue to check for errors in release builds,
        // but continue the build even when errors are found:
        disable += "MissingTranslation"
        abortOnError = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources.excludes += "META-INF/LICENSE"
        resources.excludes += "META-INF/com.google.dagger_dagger.version"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    //modules
    lintChecks(project(":lint"))

    implementation(project(":core:di"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:android"))
    implementation(project(":core:utils"))
    implementation(project(":core:ui"))

    implementation(project(":feature:mobile_services"))
    implementation(project(":feature:preferences:ui"))
    implementation(project(":feature:download:ui"))
    implementation(project(":feature:address_sorter:ui"))
    implementation(project(":feature:webview:ui"))

    implementation(project(":feature:rate"))
    implementation(project(":feature:about"))

    implementation(libs.jodaTime)

    //android
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.constraintlayout)

    //paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.runtime.ktx)

    //ui
    implementation(libs.hdodenhof.circleimageview)
    implementation(libs.yalantis.ucrop)
    implementation(libs.jaredrummler.deviceNames)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.picasso)
    implementation(libs.decoro)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.core)

    implementation(libs.okhttp)
    implementation(libs.okhttp.loggingInterceptor)

    implementation(libs.androidx.startup.runtime)

    implementation(libs.treessence)
}

fun loadSigningConfigForBuildType(propsFile: File, signingConfigName: String, shouldCreate: Boolean) {
    fun ApkSigningConfig.set(properties: Properties) {
        storeFile = File("${System.getenv("ANDROID_HOME")}/release.keystore")
        keyAlias = properties.getProperty("alias")
        keyPassword = properties.getProperty("signingPassword")
        storePassword = properties.getProperty("signingPassword")
    }

    if (propsFile.exists()) {
            val properties = Properties()
            properties.load(FileInputStream(propsFile))
            android {
                signingConfigs {
                    if (shouldCreate) {
                        create(signingConfigName) {
                            set(properties)
                        }
                    } else {
                        getByName(signingConfigName) {
                            set(properties)
                        }
                    }
                }
            }
        }
}
