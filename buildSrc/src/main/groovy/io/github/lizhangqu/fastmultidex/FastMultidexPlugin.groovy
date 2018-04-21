package io.github.lizhangqu.fastmultidex

import org.gradle.api.Plugin
import org.gradle.api.Project

class FastMultidexPlugin implements Plugin<Project> {

    @SuppressWarnings("GrMethodMayBeStatic")
    String getAndroidGradlePluginVersionCompat() {
        String version = null
        try {
            Class versionModel = Class.forName("com.android.builder.model.Version")
            def versionFiled = versionModel.getDeclaredField("ANDROID_GRADLE_PLUGIN_VERSION")
            versionFiled.setAccessible(true)
            version = versionFiled.get(null)
        } catch (Exception e) {
            version = "unknown"
        }
        return version
    }
    
    @Override
    void apply(Project project) {

    }

}