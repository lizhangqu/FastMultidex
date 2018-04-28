package io.github.lizhangqu.fastmultidex.cache

import org.gradle.api.Project

class CacheManager {
    private static final Cache localCache = new SimpleLocalCache()
    static Cache networkCache = null

    static void setNetworkCache(Cache cache) {
        networkCache = cache
    }

    static void putFile(Project project, String type, String key, File srcFile) {
        if (localCache != null) {
            localCache.putFile(type, key, srcFile)
        }

        if (networkCache != null) {
            networkCache.putFile(type, key, srcFile)
        }
    }

    static void fetchFile(Project project, String type, String key, File destFile) {
        if (localCache != null) {
            localCache.fetchFile(type, key, destFile)
        }

        if (!destFile.exists() && networkCache != null) {
            networkCache.fetchFile(type, key, destFile)

            if (destFile.exists() && destFile.length() > 0) {
                if (localCache != null) {
                    localCache.putFile(type, key, destFile)
                }
            }
        }
    }

    static void clearAll(Project project) {
        if (localCache != null) {
            localCache.clearAll()
        }

        if (networkCache != null) {
            networkCache.clearAll()
        }
    }

    static void clearLocal(Project project) {
        if (localCache != null) {
            localCache.clearAll()
        }

    }

    static void clearNetwork(Project project) {
        if (networkCache != null) {
            networkCache.clearAll()
        }

    }

}
