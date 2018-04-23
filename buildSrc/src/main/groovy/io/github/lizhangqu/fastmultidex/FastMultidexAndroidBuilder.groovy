package io.github.lizhangqu.fastmultidex

import com.android.build.gradle.internal.transforms.JarMerger
import com.android.build.gradle.internal.variant.ApplicationVariantData
import com.android.builder.core.AndroidBuilder
import com.android.builder.core.DexOptions
import com.android.builder.core.ErrorReporter
import com.android.ide.common.process.JavaProcessExecutor
import com.android.ide.common.process.ProcessException
import com.android.ide.common.process.ProcessExecutor
import com.android.ide.common.process.ProcessOutputHandler
import com.android.utils.ILogger
import com.android.xml.AndroidXPathFactory
import io.github.lizhangqu.fastmultidex.cache.CacheManager
import javassist.ClassPool
import javassist.CtClass
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.util.GFileUtils
import org.xml.sax.InputSource

import javax.xml.xpath.XPath
import javax.xml.xpath.XPathExpressionException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry;

class FastMultidexAndroidBuilder extends AndroidBuilder {
    private Project project
    private ApplicationVariantData applicationVariantData
    private AndroidBuilder androidBuilder

    private JavaProcessExecutor javaProcessExecutor

    public static final String CACHE_TYPE_PRE_DEX = "pre-dex-1.0.0"

    FastMultidexAndroidBuilder(Project project, ApplicationVariantData applicationVariantData, AndroidBuilder androidBuilder, String projectId, String createdBy, ProcessExecutor processExecutor, JavaProcessExecutor javaProcessExecutor, ErrorReporter errorReporter, ILogger logger, boolean verboseExec) {
        super(projectId, createdBy, processExecutor, javaProcessExecutor, errorReporter, logger, verboseExec)
        this.project = project
        this.applicationVariantData = applicationVariantData
        this.androidBuilder = androidBuilder
        this.javaProcessExecutor = javaProcessExecutor
    }

    @Override
    void convertByteCode(Collection<File> inputs, File outDexFolder,
                         final boolean multidex, File mainDexList,
                         final DexOptions dexOptions,
                         final ProcessOutputHandler processOutputHandler) throws IOException, InterruptedException, ProcessException {
        project.logger.info("=======convertByteCode start======");
        inputs.each {
            project.logger.info("inputs ${it}")
        }
        project.logger.info("outDexFolder ${outDexFolder}")
        project.logger.info("multidex ${multidex}")
        project.logger.info("mainDexList ${mainDexList}")
        project.logger.info("dexOptions ${dexOptions}")
        project.logger.info("processOutputHandler ${processOutputHandler}")
        project.logger.info("=======convertByteCode end======");
        Profiler.start()

        Profiler.enter("mainDexList")
        Collection<String> newMainDexList = getMainDexList(inputs)
        Profiler.release()

        Profiler.enter("repackage")
        Collection<File> repackageInputs = repackage(inputs, newMainDexList)
        Profiler.release()

        Profiler.enter("jar2dex")
        Collection<File> jar2DexFiles = jar2dex(repackageInputs, multidex, dexOptions, processOutputHandler)
        Profiler.release()

        Profiler.enter("copyDex")
        AtomicInteger atomicInteger = new AtomicInteger()
        jar2DexFiles.each { File dexDir ->
            File[] dexFiles = dexDir.listFiles(new FilenameFilter() {
                @Override
                boolean accept(File dir, String name) {
                    return name.endsWith(".dex")
                }
            })
            if (dexDir.getName().startsWith("mainDex")) {
                dexFiles.each {
                    GFileUtils.copyFile(it, new File(outDexFolder, "classes.dex"))
                    atomicInteger.incrementAndGet()
                }
            } else {
                dexFiles.each {
                    GFileUtils.copyFile(it, new File(outDexFolder, "classes${atomicInteger.incrementAndGet()}.dex"))
                }
            }
        }
        Profiler.release()
        Profiler.release()

        project.logger.error(Profiler.dump())
    }

    @Override
    void preDexLibrary(File inputFile, File outFile, boolean multiDex, DexOptions dexOptions, ProcessOutputHandler processOutputHandler) throws IOException, InterruptedException, ProcessException {
        String md5 = null
        File dexFile = new File(outFile, "classes.dex")
        if (inputFile.getName().endsWith(".jar") && inputFile.isFile()) {
            if (inputFile.isFile()) {
                md5 = getFileMD5(inputFile)
            } else if (inputFile.isDirectory()) {
                inputFile.eachFileRecurse { File file ->
                    md5 = getMD5(md5 + getFileMD5(file))
                }
            }
            md5 = getMD5(md5 + dexOptions.getJumboMode() + dexOptions.getKeepRuntimeAnnotatedClasses())

            if (md5 != null && md5.length() > 0) {
                CacheManager.fetchFile(CACHE_TYPE_PRE_DEX, md5, dexFile)
            }

            if (dexFile.exists() && dexFile.length() > 0) {
                return
            }
        }

        super.preDexLibrary(inputFile, outFile, multiDex, dexOptions, processOutputHandler)

        if (md5 != null && md5.length() > 0 && dexFile.exists()) {
            CacheManager.putFile(CACHE_TYPE_PRE_DEX, md5, dexFile)
        }
    }

    Collection<String> getMainDexList(Collection<File> inputs) {
        Set<String> mainDexList = new HashSet<String>()
        if (inputs == null || inputs.size() == 0) {
            return mainDexList
        }
        File manifestFile = applicationVariantData.getMainOutput().processResourcesTask.getManifestFile()
        String applicationName = getApplicationName(manifestFile)

        ClassPool classPool = new ClassPool()
        inputs.each {
            if (it.isFile()) {
                classPool.insertClassPath(it.getAbsolutePath())
            } else {
                classPool.appendClassPath(it.getAbsolutePath())
            }
        }

        Set<String> rootClasses = new LinkedHashSet<>()
        rootClasses.add(applicationName)

        rootClasses.each {
            addRefClazz(classPool, it, mainDexList, "");
        }

        List<String> mainDexListClass = new ArrayList<String>()
        mainDexList.each {
            mainDexListClass.add(it.replaceAll("\\.", "/") + ".class")
        }

        File intermediatesDir = this.applicationVariantData.getScope().getGlobalScope().getIntermediatesDir()
        File mainDexListFile = new File(intermediatesDir, "fastmultidex/${this.applicationVariantData.getVariantConfiguration().getDirName()}/mainDexList.txt")
        GFileUtils.deleteQuietly(mainDexListFile)
        GFileUtils.touch(mainDexListFile)
        FileUtils.writeLines(mainDexListFile, mainDexList)
        return mainDexListClass
    }

    Collection<File> repackage(Collection<File> inputs, Collection<String> mainDexList) throws IOException {
        if (inputs == null || inputs.size() == 0) {
            return null
        }
        File intermediatesDir = this.applicationVariantData.getScope().getGlobalScope().getIntermediatesDir()
        File repackageDir = new File(intermediatesDir, "fastmultidex/${this.applicationVariantData.getVariantConfiguration().getDirName()}")
        GFileUtils.deleteDirectory(repackageDir)
        GFileUtils.mkdirs(repackageDir)

        List<File> jars = new ArrayList<>()
        List<File> folders = new ArrayList<>()
        inputs.each {
            if (it.isDirectory()) {
                folders.add(it)
            } else {
                jars.add(it)
            }
        }

        if (!folders.isEmpty()) {
            File mergedJar = new File(repackageDir, "jarmerge/combined.jar")
            GFileUtils.deleteQuietly(mergedJar)
            GFileUtils.touch(mergedJar)
            JarMerger jarMerger = new JarMerger(mergedJar)
            folders.each {
                jarMerger.addFolder(it)
            }
            jarMerger.close()
            if (mergedJar.length() > 0) {
                jars.add(mergedJar)
            }
        }

        List<File> result = new ArrayList<>()
        File mainDexJar = new File(repackageDir, "repackage/mainDex.jar")
        GFileUtils.deleteQuietly(mainDexJar)
        GFileUtils.touch(mainDexJar)
        JarOutputStream mainDexJarOutputStream = new JarOutputStream(
                new BufferedOutputStream(new FileOutputStream(mainDexJar)));

        jars.each { File jarFile ->
            //calculate dest file
            String md5Value = getMD5(jarFile.getAbsolutePath())
            File destFile = new File(repackageDir, "repackage/${jarFile.getName() - ".jar"}_${md5Value}.jar")

            //add
            result.add(destFile)

            JarOutputStream destJarOutputStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(destFile)))
            JarFile sourceJarFile = new JarFile(jarFile)

            //copy to main dex jar
            List<String> addedMainDexList = new ArrayList<>();
            sourceJarFile.entries().each { JarEntry jarEntry ->
                String pathName = jarEntry.getName()
                if (mainDexList.contains(pathName)) {
                    copyStream(sourceJarFile.getInputStream(jarEntry), mainDexJarOutputStream, jarEntry, pathName)
                    addedMainDexList.add(pathName)
                }
            }

            //copy to dest jar
            if (!addedMainDexList.isEmpty()) {
                sourceJarFile.entries().each { JarEntry jarEntry ->
                    String pathName = jarEntry.getName()
                    if (!addedMainDexList.contains(pathName)) {
                        copyStream(sourceJarFile.getInputStream(jarEntry), destJarOutputStream, jarEntry, pathName);
                    }
                }
            }

            //close
            closeQuitely(sourceJarFile)
            closeQuitely(destJarOutputStream)

            //if not copy to main dex jar, then copy single file to dest
            if (addedMainDexList.isEmpty()) {
                GFileUtils.copyFile(jarFile, destFile)
            }
        }

        //close
        closeQuitely(mainDexJarOutputStream)

        //add
        result.add(0, mainDexJar);
        return result
    }

    private Collection<File> jar2dex(Collection<File> repackageInputs, boolean multidex, DexOptions dexOptions, ProcessOutputHandler processOutputHandler) {
        File intermediatesDir = this.applicationVariantData.getScope().getGlobalScope().getIntermediatesDir()
        File tmpDir = new File(intermediatesDir, "fastmultidex/${this.applicationVariantData.getVariantConfiguration().getDirName()}/jar2dex")
        GFileUtils.mkdirs(tmpDir)
        List<File> outputs = new ArrayList<>()
        ExecutorServicesHelper executorServicesHelper = new ExecutorServicesHelper(project, "jar2dex",
                repackageInputs.size() > 8 ? 8
                        : repackageInputs.size());

        List<Runnable> runnableArrayList = new ArrayList<>()
        repackageInputs.each {
            final File dexDir = getDexOutputDir(it, tmpDir)
            GFileUtils.mkdirs(dexDir)
            runnableArrayList.add(new Runnable() {
                @Override
                public void run() {
                    try {
                        preDexLibrary(it, dexDir, multidex, dexOptions, processOutputHandler)
                    } catch (Exception e) {
                        throw new GradleException(e.getMessage(), e)
                    }
                }
            })
            outputs.add(dexDir)
        }
        executorServicesHelper.execute(runnableArrayList)
        return outputs
    }

    void addRefClazz(ClassPool classPool, String clazz, Set<String> classList, String root) {
        if (classList.contains(clazz)) {
            return
        }
        try {
            CtClass ctClass = classPool.get(clazz)
            if (null != ctClass) {
                project.logger.info("[MainDex] add " + clazz + " to main dex list referenced by " + root)
                classList.add(clazz)
                if (classList.size() > 3000) {
                    return
                }
                Collection<String> references = ctClass.getRefClasses()

                if (null == references) {
                    return
                }

                references.each {
                    addRefClazz(classPool, it, classList, root + "->" + clazz);
                }
            }
        } catch (Throwable e) {
        }
    }

    static void copyStream(InputStream inputStream, JarOutputStream jos, JarEntry ze, String pathName) {
        try {
            ZipEntry newEntry = new ZipEntry(pathName)
            if (ze.getTime() != -1) {
                newEntry.setTime(ze.getTime())
                newEntry.setCrc(ze.getCrc())
            }
            jos.putNextEntry(newEntry);
            IOUtils.copy(inputStream, jos)
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            closeQuitely(inputStream)
        }
    }

    static String getApplicationName(File manifestFile) {
        XPath xpath = AndroidXPathFactory.newXPath()
        try {
            return xpath.evaluate("/manifest/application/@android:name",
                    new InputSource(new FileInputStream(manifestFile)))
        } catch (XPathExpressionException e) {
            // won't happen.
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e)
        }
        return null
    }

    static File getDexOutputDir(File input, File rootDir) {
        return new File(rootDir, input.getName() - ".jar")
    }

    static void closeQuitely(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (Exception e) {

            }
        }
    }

    static String getMD5(String str) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5")
            md5.update(str.getBytes("UTF-8"))
            BigInteger bi = new BigInteger(1, md5.digest())
            return String.format("%032x", bi).toLowerCase()
        } catch (Exception e) {

        }
        return null
    }

    static String getFileMD5(File file) {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            MappedByteBuffer byteBuffer = fileInputStream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            return String.format("%032x", bi).toLowerCase()
        } catch (Exception e) {
        } finally {
            closeQuitely(fileInputStream)
        }
        return null
    }

}
