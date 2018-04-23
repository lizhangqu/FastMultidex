package io.github.lizhangqu.fastmultidex

class CacheManager {
    private static final Cache localCache = new SimpleLocalCache()

    static boolean putFile(String type, String key, File srcFile) {
        return localCache.putFile(type, key, srcFile)
    }

    static boolean fetchFile(String type, String key, File destFile) {
        return localCache.fetchFile(type, key, destFile)
    }

}
