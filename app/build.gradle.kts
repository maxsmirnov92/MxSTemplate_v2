import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationVariantDimension
import dagger.hilt.android.plugin.util.capitalize
//import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Locale
import java.util.Properties
import java.util.regex.Matcher
import java.util.regex.Pattern

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

val gradleTaskNames: List<String> = gradle.startParameter.taskNames
val gradleTaskRequests: List<TaskExecutionRequest> = gradle.startParameter.taskRequests

logger.info("=== Running tasks: $gradleTaskNames ===")

if (isHuaweiBuild() == true) {
    logger.info("Applying plugin: com.huawei.agconnect")

    with(pluginManager) {
        apply("com.huawei.agconnect")
    }
}

data class AppVersion(
    val code: Int,
    val name: String,
    val type: String,
) {

    constructor(code: Int, type: String) : this(code, getVersionName(code), type)
}

val appVersion = AppVersion(1, "common")

android {
    namespace = "net.maxsmr.mxstemplate"

    defaultConfig {
        applicationId = "net.maxsmr.mxstemplate"
        versionCode = appVersion.code
        versionName = appVersion.name
        project.ext.set("archivesBaseName", "${project.name}_${appVersion.name}_${appVersion.type}")

        buildConfigField("int", "PROTOCOL_VERSION", "1")

        val appProperties = Properties()
        appProperties.load(FileInputStream(File(rootDir, "app/app.properties")))

        buildConfigField(
            "String",
            "AUTHORIZATION_RADAR_IO",
            "\"${appProperties.getProperty("authorizationRadarIo")}\""
        )
        buildConfigField("String", "DEV_EMAIL_ADDRESS", "\"${appProperties.getProperty("devEmailAddress")}\"")

        val donateProperties = Properties()
        donateProperties.load(FileInputStream(File(rootDir, "app/donate.properties")))
        val addressesMap = mutableMapOf<String, String>()
        donateProperties.entries.forEach {
            val key = it.key as String? ?: return@forEach
            val value = it.value as String? ?: return@forEach
            addressesMap[key] = value
        }

        var addressesHashMap = "new java.util.HashMap<String, String>()" + "{" + "{ "
        addressesMap.forEach { (k, v) -> addressesHashMap += "put(\"${k}\"," + "\"${v}\"" + ");" }
        val addressesString = "$addressesHashMap}}"

        buildConfigField(
            "java.util.Map<String, String>",
            "DEV_PAYMENT_ADDRESSES",
            addressesString
        )

        buildConfigField("String", "MOBILE_BUILD_TYPE", "\"${appVersion.type}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

//    signingConfigs {
//    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            multiDexKeepProguard = file("multidex-config.pro")
//            signingConfig = null
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            multiDexKeepProguard = file("multidex-config.pro")
//            configure<CrashlyticsExtension> {
//                mappingFileUploadEnabled = true
//            }

            applySigningConfig(
                signingConfigs,
                "release",
                File(rootDir, "app/keystore.properties"),
            )

//            val configureSigningTask = tasks.register("configureSigning") {
//                doFirst {
//                    // создавать конфиг в таком месте запрещается;
            // выставление signingConfig сработает, но подписи apk не будет
//                }
//            }
//            tasks.matching { it.name.endsWith("Release") }.configureEach {
//                dependsOn(configureSigningTask)
//            }
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
        }

        /**
         * Релизный вариант сборки
         */
        getByName("appProd") {
//            firebaseAppDistribution {
//                groups = "App"
//                releaseNotesFile = "app/notes.txt"
//            }
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val flavour = variant.flavorName
                val buildTypeName = variant.buildType.name
                val versionName = variant.versionName
                output.outputFileName =
                    "${flavour}${buildTypeName.capitalize()}_${versionName}_${appVersion.type}.apk"
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
        buildConfig = true
    }

//    sourceSets {
//        named("main") {
//            java.srcDir("")
//        }
//    }
}

dependencies {
    //modules
//    lintChecks(project(":lint"))

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
//    implementation(project(":feature:address_sorter:ui"))
    implementation(project(":feature:webview:ui"))

    implementation(project(":feature:rate"))
    implementation(project(":feature:about"))

    implementation(libs.jodaTime)

    //android
//    implementation(libs.androidx.core.splashscreen)
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

    implementation(libs.r8)

    //firebase
//    implementation(libs.firebase.analytics)
//    implementation(libs.firebase.crashlytics)

    // debugImplementation because LeakCanary should only run in debug builds.
//    debugImplementation(libs.leakCanary)
}


fun ApplicationVariantDimension.applySigningConfig(
    signingConfigs: NamedDomainObjectCollection<out ApkSigningConfig>,
    signingConfigName: String,
    propsFile: File,
) {
    check(signingConfigName.isNotEmpty()) {
        "Name for signingConfig is not specified"
    }

    fun createSigningConfig(): Boolean {
        if (!propsFile.isFile || !propsFile.exists()) {
            logger.warn("No valid properties file ('$propsFile') for \"$signingConfigName\"")
            return false
        }

        val properties = Properties()
        properties.load(FileInputStream(propsFile))
        android {
            signingConfigs {
                create(signingConfigName) {
                    storeFile = File("${System.getenv("ANDROID_HOME")}/$signingConfigName.keystore")
                    keyAlias = properties.getProperty("alias")
                    keyPassword = properties.getProperty("signingPassword")
                    storePassword = properties.getProperty("signingPassword")
                }
                logger.info("Signing config \"$signingConfigName\" created")
            }
        }
        return true
    }

    var config = signingConfigs.findByName(signingConfigName)
    if (config == null) {
        // не найдено - пробуем создать, если есть валидный файл с properties
        if (createSigningConfig()) {
            config = signingConfigs.findByName(signingConfigName)
                ?: throw IllegalStateException("Cannot create signingConfig \"$signingConfigName\"")
        }
    }
    if (config != null) {
        logger.info("Applying signingConfig \"$signingConfigName\"")
        signingConfig = config
    }
}

/**
 * Проверка всех возможных google-флаворов от 'app'
 * исходя из запущенной таски
 */
fun isGoogleBuild(): Boolean? {
    return containsFlavors(arrayOf("appProdGoogle", "appDevGoogle"))
}

fun isHuaweiBuild(): Boolean? {
    return containsFlavors(arrayOf("appProdHuawei", "appDevHuawei"))
}

fun containsFlavors(flavors: Array<String>): Boolean? {
    val taskNames = gradleTaskNames
    if (taskNames.isEmpty()) {
        return null
    }
    return taskNames.any { task ->
        flavors.any { flavorName ->
            task.contains(flavorName, true)
        }
    }
}

fun isDevBuild(): Boolean? {
    val taskNames = gradleTaskNames
    if (taskNames.isEmpty()) {
        return null
    }
    return taskNames.any { task ->
        task.contains("dev", true)
    }
}

fun getVersionName(versionCode: Int): String {
    val postfix = if (isDevBuild() != false) {
        "dev"
    } else {
        "prod"
    }
    val buildType = getCurrentBuildType()
    var result = "1.0$versionCode.${postfix}"
    if (buildType.isNotEmpty()) {
        result += buildType.capitalize()
    }
    return result
}

fun getCurrentFlavorName(): String {
    val matcher = getTasksMatcher()
    return if (matcher.find())
        matcher.group(1).lowercase(Locale.getDefault())
    else {
        ""
    }
}

fun getCurrentBuildType(): String {
    val matcher = getTasksMatcher()
    return if (matcher.find())
        matcher.group(2).lowercase(Locale.getDefault())
    else {
        ""
    }
}

fun getCurrentApplicationId(): String {
    val currFlavor: String = getCurrentFlavorName()
    return android.productFlavors.find { flavor ->
        flavor.name == currFlavor
    }?.applicationId ?: ""
}

fun getTasksMatcher(): Matcher {
    val requests = gradleTaskRequests.toString()
    val pattern = if (requests.contains("assemble")) {
        // to run ./gradlew assembleRelease to build APK
        Pattern.compile("assemble(\\w+)(Release|Debug)")
    } else if (requests.contains("bundle")) {// to run ./gradlew bundleRelease to build .aab
        Pattern.compile("bundle(\\w+)(Release|Debug)")
    } else {
        Pattern.compile("generate(\\w+)(Release|Debug)")
    }
    return pattern.matcher(requests)
}