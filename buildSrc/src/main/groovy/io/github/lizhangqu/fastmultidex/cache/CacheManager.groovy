package io.github.lizhangqu.fastmultidex.cache

import org.gradle.api.Project

public class CacheManager {

    private volatile static CacheManager sInstance;
    private final Cache localCache = new SimpleLocalCache()
    Cache networkCache = null

    private CacheManager() {
    }

    public static CacheManager getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        if (null == sInstance) {
            synchronized (CacheManager.class) {
                if (null == sInstance) {
                    sInstance = new CacheManager();
                }
            }
        }
        return sInstance;
    }

    void setNetworkCache(Cache cache) {
        networkCache = cache
    }

    void putFile(Project project, String type, String key, File srcFile) {
        if (localCache != null) {
            localCache.putFile(type, key, srcFile)
        }

        if (networkCache != null) {
            networkCache.putFile(type, key, srcFile)
        }
    }

    void fetchFile(Project project, String type, String key, File destFile) {
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

    void clearAll() {
        if (localCache != null) {
            localCache.clearAll()
        }

        if (networkCache != null) {
            networkCache.clearAll()
        }
    }

    void clearLocal() {
        if (localCache != null) {
            localCache.clearAll()
        }

    }

    void clearNetwork() {
        if (networkCache != null) {
            networkCache.clearAll()
        }

    }

}
