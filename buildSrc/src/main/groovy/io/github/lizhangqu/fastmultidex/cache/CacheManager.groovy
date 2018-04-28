package io.github.lizhangqu.fastmultidex.cache

import org.gradle.api.Project

public class CacheManager {

    private volatile static CacheManager sInstance;
    private final Cache localCache = new SimpleLocalCache()
    private Cache networkCache = null
    private boolean networkCacheUploadEnabled = false
    private boolean networkCacheDownloadEnabled = false

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

    public void setNetworkCache(Cache cache) {
        networkCache = cache
    }

    public void setNetworkCacheUploadEnabled(boolean enabled) {
        networkCacheUploadEnabled = enabled
    }

    public void setNetworkCacheDownloadEnabled(boolean enabled) {
        networkCacheDownloadEnabled = enabled
    }

    public void putFile(Project project, String type, String key, File srcFile) {
        if (localCache != null) {
            localCache.putFile(type, key, srcFile)
        }

        if (networkCache != nul && networkCacheUploadEnabled) {
            networkCache.putFile(type, key, srcFile)
        }
    }

    public void fetchFile(Project project, String type, String key, File destFile) {
        if (localCache != null) {
            localCache.fetchFile(type, key, destFile)
        }

        if (!destFile.exists() && networkCache != null && networkCacheDownloadEnabled) {
            networkCache.fetchFile(type, key, destFile)

            if (destFile.exists() && destFile.length() > 0) {
                if (localCache != null) {
                    localCache.putFile(type, key, destFile)
                }
            }
        }
    }

    public void clearAll() {
        if (localCache != null) {
            localCache.clearAll()
        }

        if (networkCache != null) {
            networkCache.clearAll()
        }
    }

    public void clearLocal() {
        if (localCache != null) {
            localCache.clearAll()
        }

    }

    public void clearNetwork() {
        if (networkCache != null) {
            networkCache.clearAll()
        }

    }

}
