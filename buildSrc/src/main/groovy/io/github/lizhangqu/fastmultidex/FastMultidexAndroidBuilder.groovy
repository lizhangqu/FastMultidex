package io.github.lizhangqu.fastmultidex

import com.android.builder.core.AndroidBuilder
import com.android.builder.core.DexByteCodeConverter
import com.android.builder.core.DexOptions
import com.android.builder.core.ErrorReporter
import com.android.ide.common.process.JavaProcessExecutor
import com.android.ide.common.process.ProcessException
import com.android.ide.common.process.ProcessExecutor
import com.android.ide.common.process.ProcessOutputHandler
import com.android.utils.ILogger
import org.gradle.api.Project;

class FastMultidexAndroidBuilder extends AndroidBuilder {
    private Project project

    protected AndroidBuilder androidBuilder

    private JavaProcessExecutor javaProcessExecutor
    /**
     * Creates an AndroidBuilder.
     * <p>
     * <var>verboseExec</var> is needed on top of the ILogger due to remote exec tools not being
     * able to output info and verbose messages separately.
     *
     * @param projectId @param createdBy the createdBy String for the apk manifest.
     * @param processExecutor @param javaProcessExecutor @param errorReporter @param logger the Logger
     * @param verboseExec whether external tools are launched in verbose mode
     */
    FastMultidexAndroidBuilder(Project project, AndroidBuilder androidBuilder, String projectId, String createdBy, ProcessExecutor processExecutor, JavaProcessExecutor javaProcessExecutor, ErrorReporter errorReporter, ILogger logger, boolean verboseExec) {
        super(projectId, createdBy, processExecutor, javaProcessExecutor, errorReporter, logger, verboseExec)
        this.project = project
        this.androidBuilder = androidBuilder
        this.javaProcessExecutor = javaProcessExecutor
    }

    @Override
    void convertByteCode(Collection<File> inputs, File outDexFolder, boolean multidex, File mainDexList, DexOptions dexOptions, ProcessOutputHandler processOutputHandler) throws IOException, InterruptedException, ProcessException {
        project.logger.error("=======convertByteCode start======");
        inputs.each {
            project.logger.error("inputs ${it}")
        }
        project.logger.error("outDexFolder ${outDexFolder}")
        project.logger.error("multidex ${multidex}")
        project.logger.error("mainDexList ${mainDexList}")
        project.logger.error("dexOptions ${dexOptions}")
        project.logger.error("processOutputHandler ${processOutputHandler}")
        project.logger.error("=======convertByteCode end======");
        super.convertByteCode(inputs, outDexFolder, multidex, mainDexList, dexOptions, processOutputHandler)
    }

    @Override
    DexByteCodeConverter getDexByteCodeConverter() {
        project.logger.error("=======getDexByteCodeConverter start======");
        def converter = super.getDexByteCodeConverter();
        project.logger.error("converter ${converter}")
        project.logger.error("=======getDexByteCodeConverter end======");
        return converter
    }
}
