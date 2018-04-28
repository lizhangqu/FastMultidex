package io.github.lizhangqu.fastmultidex.cache

import org.gradle.api.Project

class CacheManager {
    private static final Cache localCache = new SimpleLocalCache()
    static Cache netCache = null

    static void setNetCache(Cache netCache) {
        CacheManager.netCache = netCache
    }

    static void putFile(Project project, String type, String key, File srcFile) {
        if (localCache != null) {
            localCache.putFile(type, key, srcFile)
        }


        if (netCache != null) {
            netCache.putFile(type, key, srcFile)
        }
    }

    static void fetchFile(Project project, String type, String key, File destFile) {
        if (localCache != null) {
            localCache.fetchFile(type, key, destFile)
        }

        if (!destFile.exists() && netCache != null) {
            netCache.fetchFile(type, key, destFile)
        }
    }

    static void clearAll(Project project) {
        if (localCache != null) {
            localCache.clearAll()
        }

        if (netCache != null) {
            netCache.clearAll()
        }
    }

}
