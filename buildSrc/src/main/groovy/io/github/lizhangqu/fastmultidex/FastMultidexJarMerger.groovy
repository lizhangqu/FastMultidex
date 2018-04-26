package io.github.lizhangqu.fastmultidex

import com.android.annotations.NonNull
import com.android.build.gradle.internal.transforms.JarMerger

import java.util.jar.JarEntry;

/**
 * @author lizhangqu
 * @version V1.0
 * @since 2018-04-26 13:30
 */
class FastMultidexJarMerger extends JarMerger {
    FastMultidexJarMerger(File jarFile) throws IOException {
        super(jarFile)
    }

    @Override
    void addEntry(@NonNull String path, @NonNull byte[] bytes) throws IOException {
        init();

        final JarEntry jarEntry = new JarEntry(entryPath)
        jarEntry.setLastModifiedTime(ZERO_TIME)
        jarEntry.setLastAccessTime(ZERO_TIME)
        jarEntry.setCreationTime(ZERO_TIME)

        jarOutputStream.putNextEntry(jarEntry)
        jarOutputStream.write(bytes)
        jarOutputStream.closeEntry()
    }

}
