package io.github.lizhangqu.fastmultidex

import org.gradle.util.GFileUtils

class SimpleLocalCache implements Cache {

    final File cacheRootDir = new File(System.getProperty("user.home"), ".fastmultidex/cache")

    SimpleLocalCache() {
        GFileUtils.mkdirs(cacheRootDir)
    }

    File getLocalCacheFile(String type, String key) {
        return new File(cacheRootDir, type + "/" + key);
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
        File cacheFile = getLocalCacheFile(type, key)
        if (cacheFile.exists()) {
            return true
        }

        if (srcFile.isFile()) {
            GFileUtils.touch(cacheFile)
            GFileUtils.copyFile(srcFile, cacheFile)
            return true
        } else {
            GFileUtils.mkdirs(cacheFile)
            GFileUtils.copyDirectory(srcFile, cacheFile)
            return true
        }
    }

    boolean fetchFile(String type, String key, File destFile) {
        if (type == null || type.length() == 0) {
            return false
        }
        if (key == null || key.length() == 0) {
            return false
        }
        File cacheFile = getLocalCacheFile(type, key)
        if (!cacheFile.exists()) {
            return false
        }

        if (cacheFile.isFile()) {
            GFileUtils.touch(destFile)
            GFileUtils.copyFile(cacheFile, destFile)
        } else {
            GFileUtils.mkdirs(destFile)
            GFileUtils.copyDirectory(cacheFile, destFile)
        }
    }

}
