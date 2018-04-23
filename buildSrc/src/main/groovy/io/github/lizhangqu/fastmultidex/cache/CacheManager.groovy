package io.github.lizhangqu.fastmultidex.cache

class CacheManager {
    private static final Cache localCache = new SimpleLocalCache()

    static boolean putFile(String type, String key, File srcFile) {
        return localCache.putFile(type, key, srcFile)
    }

    static boolean fetchFile(String type, String key, File destFile) {
        return localCache.fetchFile(type, key, destFile)
    }

    static void clear(String type) {
        localCache.clear(type)
    }

    static void clearAll() {
        localCache.clearAll()
    }

}
