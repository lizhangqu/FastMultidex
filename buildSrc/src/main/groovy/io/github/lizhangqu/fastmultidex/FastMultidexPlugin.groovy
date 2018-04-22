package io.github.lizhangqu.fastmultidex

import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.core.GradleVariantConfiguration
import com.android.build.gradle.internal.incremental.InstantRunBuildContext
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.DexTransform
import com.android.build.gradle.internal.variant.ApplicationVariantData
import com.android.builder.core.AndroidBuilder
import com.android.builder.core.DefaultDexOptions
import com.android.builder.core.DexOptions
import com.android.builder.core.ErrorReporter
import com.android.builder.core.LibraryRequest
import com.android.builder.core.VariantConfiguration
import com.android.builder.model.BuildType
import com.android.builder.model.ProductFlavor
import com.android.builder.utils.FileCache
import com.android.ide.common.process.JavaProcessExecutor
import com.android.ide.common.process.ProcessExecutor
import com.android.utils.ILogger
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskCollection

import java.lang.reflect.Field
import java.lang.reflect.Modifier

class FastMultidexPlugin implements Plugin<Project> {

    static String getAndroidGradlePluginVersionCompat() {
        String version = null
        try {
            Class versionModel = Class.forName("com.android.builder.model.Version")
            def versionFiled = versionModel.getDeclaredField("ANDROID_GRADLE_PLUGIN_VERSION")
            versionFiled.setAccessible(true)
            version = versionFiled.get(null)
        } catch (Exception e) {
            version = "unknown"
        }
        return version
    }

    static List<TransformTask> findTransformTaskByTransformType(Project project, VariantConfiguration variantConfiguration, Class<?> transformClass) {
        List<TransformTask> transformTasksList = new ArrayList<>()
        TaskCollection<TransformTask> transformTasks = project.getTasks().withType(TransformTask.class)
        SortedMap<String, TransformTask> transformTaskSortedMap = transformTasks.getAsMap()
        String variantName = variantConfiguration.getFullName()
        transformTaskSortedMap.each { String taskName, TransformTask transformTask ->
            if (variantName == transformTask.getVariantName()) {
                if (transformTask.getTransform().getClass() == transformClass) {
                    transformTasksList.add(transformTask)
                }
            }
        }
        return transformTasksList
    }

    @Override
    void apply(Project project) {
        String androidGradlePluginVersionCompat = getAndroidGradlePluginVersionCompat()
        //only 2.+ should do it. 3.+ don't need it
        if (androidGradlePluginVersionCompat.startsWith("2.")) {
            project.afterEvaluate {
                AppExtension appExtension = project.getExtensions().findByType(AppExtension.class)
                appExtension.applicationVariants.all { def variant ->
                    //get ApplicationVariantData and GradleVariantConfiguration
                    ApplicationVariantData applicationVariantData = variant.getMetaClass().getProperty(variant, 'variantData')
                    GradleVariantConfiguration variantConfiguration = applicationVariantData.getVariantConfiguration()
                    String fullName = variantConfiguration.getFullName()
                    //only debug build enable it
                    if (fullName.toLowerCase().contains("debug")) {
                        BuildType buildType = variantConfiguration.getBuildType()
                        ProductFlavor productFlavor = variantConfiguration.getMergedFlavor()
                        if (buildType.getMultiDexEnabled() || productFlavor.getMultiDexEnabled()) {
                            throw new GradleException("You must disable multidex in build.gradle when use fast multidex. Like this config:\n\n" +
                                    "defaultConfig {\n" +
                                    "    multiDexEnabled false\n" +
                                    "}\n" +
                                    "\n" +
                                    "buildTypes {\n" +
                                    "    debug {\n" +
                                    "        multiDexEnabled false\n" +
                                    "    }\n" +
                                    "    release {\n" +
                                    "        multiDexEnabled true\n" +
                                    "    }\n" +
                                    "}")
                        }

                        //find dex transform task wrapper
                        List<TransformTask> transformTaskList = findTransformTaskByTransformType(project, variantConfiguration, DexTransform.class)
                        transformTaskList.each { TransformTask transformTask ->
                            //get dex transform
                            DexTransform dexTransform = transformTask.transform

                            //get dex options
                            DefaultDexOptions dexOptions = dexTransform.getMetaClass().getProperty(dexTransform, 'dexOptions')
                            dexOptions.setPreDexLibraries(false)

                            //get dex transform original filed
                            boolean debugMode = dexTransform.getMetaClass().getProperty(dexTransform, 'debugMode')
                            boolean multiDex = dexTransform.getMetaClass().getProperty(dexTransform, 'multiDex')
                            File mainDexListFile = dexTransform.getMetaClass().getProperty(dexTransform, 'mainDexListFile')
                            File intermediateFolder = dexTransform.getMetaClass().getProperty(dexTransform, 'intermediateFolder')
                            AndroidBuilder androidBuilder = dexTransform.getMetaClass().getProperty(dexTransform, 'androidBuilder')
                            InstantRunBuildContext instantRunBuildContext = dexTransform.getMetaClass().getProperty(dexTransform, 'instantRunBuildContext')
                            Optional<FileCache> buildCache = dexTransform.getMetaClass().getProperty(dexTransform, 'buildCache')

                            //get android builder original field
                            String mProjectId = androidBuilder.getMetaClass().getProperty(androidBuilder, 'mProjectId')
                            String mCreatedBy = androidBuilder.getMetaClass().getProperty(androidBuilder, 'mCreatedBy')
                            ProcessExecutor mProcessExecutor = androidBuilder.getMetaClass().getProperty(androidBuilder, 'mProcessExecutor')
                            JavaProcessExecutor mJavaProcessExecutor = androidBuilder.getMetaClass().getProperty(androidBuilder, 'mJavaProcessExecutor')
                            ErrorReporter mErrorReporter = androidBuilder.getMetaClass().getProperty(androidBuilder, 'mErrorReporter')
                            ILogger mLogger = androidBuilder.getMetaClass().getProperty(androidBuilder, 'mLogger')
                            boolean mVerboseExec = androidBuilder.getMetaClass().getProperty(androidBuilder, 'mVerboseExec')

                            //create new android builder
                            FastMultidexAndroidBuilder fastAndroidBuilder = new FastMultidexAndroidBuilder(project, applicationVariantData, androidBuilder, mProjectId, mCreatedBy, mProcessExecutor, mJavaProcessExecutor, mErrorReporter, mLogger, mVerboseExec)
                            fastAndroidBuilder.setSdkInfo(androidBuilder.getSdkInfo())
                            fastAndroidBuilder.setTargetInfo(androidBuilder.getTargetInfo())
                            List<LibraryRequest> mLibraryRequests = androidBuilder.getMetaClass().getProperty(androidBuilder, 'mLibraryRequests')
                            fastAndroidBuilder.setLibraryRequests(mLibraryRequests)

                            //replace android builder in original dex transform
                            Field androidBuilderField = DexTransform.class.getDeclaredField("androidBuilder")
                            androidBuilderField.setAccessible(true)
                            Field modifiersField = Field.class.getDeclaredField("modifiers")
                            modifiersField.setAccessible(true)
                            modifiersField.setInt(androidBuilderField, androidBuilderField.getModifiers() & ~Modifier.FINAL)
                            androidBuilderField.setAccessible(true)
                            androidBuilderField.set(dexTransform, fastAndroidBuilder)
                            modifiersField.setInt(androidBuilderField, androidBuilderField.getModifiers() & Modifier.FINAL)

                            //create new dex transform
                            DexTransform newDexTransform = new DexTransform(dexOptions,
                                    debugMode,
                                    false,
                                    null,
                                    intermediateFolder,
                                    fastAndroidBuilder,
                                    project.getLogger(),
                                    instantRunBuildContext,
                                    buildCache)

                            //replace dex transform to new dex transform
                            Field transformField = TransformTask.class.getDeclaredField("transform")
                            transformField.setAccessible(true)
                            transformField.set(transformTask, newDexTransform)
                        }
                    }
                }
            }
        }
    }
}