package io.github.lizhangqu.fastmultidex

import com.android.annotations.NonNull
import com.android.build.gradle.internal.transforms.JarMerger

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream;

/**
 * @author lizhangqu
 * @version V1.0
 * @since 2018-04-26 13:30
 */
class FastMultidexJarMerger extends JarMerger {
    private JarOutputStream outputStream;

    FastMultidexJarMerger(File jarFile) throws IOException {
        super(jarFile)
    }

    void init() {
        if (outputStream == null) {
            Method method = JarMerger.class.getDeclaredMethod("init")
            method.setAccessible(true)
            method.invoke(this)

            Field field = JarMerger.class.getDeclaredField("jarOutputStream")
            field.setAccessible(true)
            outputStream = field.get(this)
        }

    }

    @Override
    void addEntry(@NonNull String entryPath, @NonNull byte[] bytes) throws IOException {
        init();

        final JarEntry jarEntry = new JarEntry(entryPath)
        jarEntry.setLastModifiedTime(ZERO_TIME)
        jarEntry.setLastAccessTime(ZERO_TIME)
        jarEntry.setCreationTime(ZERO_TIME)

        outputStream.putNextEntry(jarEntry)
        outputStream.write(bytes)
        outputStream.closeEntry()
    }

}
