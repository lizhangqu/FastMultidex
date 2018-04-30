package io.github.lizhangqu.fastmultidex.cache

import io.github.lizhangqu.fastmultidex.maven.Resolver
import org.eclipse.aether.artifact.Artifact
import org.gradle.api.Project
import org.gradle.util.GFileUtils

class MavenCache implements Cache {

    private static final String GROUP = "com.vdian.android.lib.fastmultidex"

    private Resolver resolver = new Resolver(project)
    private Project project

    MavenCache(Project project) {
        this.project = project
        this.resolver = new Resolver(project)
    }

    static String getGroup() {
        return GROUP
    }

    static String getArtifactId(String type) {
        return type
    }

    static String getVersion(String version) {
        if (version.endsWith("-SNAPSHOT")) {
            return version
        }
        return version + "-SNAPSHOT"
    }

    boolean putFile(String type, String key, File srcFile) {
        if (type == null || type.length() == 0) {
            return false
        }
        if (key == null || key.length() == 0) {
            return false
        }
        if (srcFile == null || !srcFile.exists()) {
            return false
        }
        String group = getGroup()
        String artifactId = getArtifactId(type)
        String version = getVersion(key)

        return true
    }

    boolean fetchFile(String type, String key, File destFile) {
        if (type == null || type.length() == 0) {
            return false
        }
        if (key == null || key.length() == 0) {
            return false
        }
        String group = getGroup()
        String artifactId = getArtifactId(type)
        String version = getVersion(key)

        Artifact artifact = resolver.resolve(group, artifactId, version)
        if (artifact == null) {
            return false
        }
        File cacheFile = artifact.getFile()
        if (cacheFile == null || cacheFile.length() == 0) {
            return false
        }
        if (cacheFile.isFile()) {
            GFileUtils.touch(destFile)
            GFileUtils.copyFile(cacheFile, destFile)
            return true
        } else {
            GFileUtils.mkdirs(destFile)
            GFileUtils.copyDirectory(cacheFile, destFile)
            return true
        }
    }

    void clearAll() {
        File baseDir = resolver.getBaseDir()
        GFileUtils.deleteQuietly(baseDir)
    }

}
