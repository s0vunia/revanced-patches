plugins {
    kotlin("jvm")
}

dependencies {
    // ReVanced patcher from JitPack
    implementation("app.revanced:revanced-patcher:21.0.0")
    implementation("com.android.tools.smali:smali:3.0.3")
}

kotlin {
    jvmToolchain(17)
}

tasks {
    jar {
        archiveBaseName.set("tiktok-patches")
        
        manifest {
            attributes(
                "Name" to "TikTok ReVanced Patches",
                "Version" to project.version.toString()
            )
        }
        
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
            exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
        }
        
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
