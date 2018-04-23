package io.github.lizhangqu.fastmultidex;

interface Cache {

    boolean putFile(String type, String key, File srcFile)

    boolean fetchFile(String type, String key, File destFile)

}
