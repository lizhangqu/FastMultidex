package io.github.lizhangqu.fastmultidex.cache;

interface Cache {

    boolean putFile(String type, String key, File srcFile)

    boolean fetchFile(String type, String key, File destFile)

    void clear(String type)

    void clearAll()

}
