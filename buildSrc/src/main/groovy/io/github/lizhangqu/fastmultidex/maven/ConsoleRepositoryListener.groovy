package io.github.lizhangqu.fastmultidex.maven

import org.gradle.api.logging.Logger;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.gradle.api.Project;

/**
 * A simplistic repository listener that logs events to the console.
 */
class ConsoleRepositoryListener
        extends AbstractRepositoryListener {
    private Logger logger;

    ConsoleRepositoryListener(Project project) {
        this.logger = project.getLogger()
    }

    void println(String msg) {
        if (logger != null) {
            logger.info(msg)
        }
    }

    void artifactDeployed(RepositoryEvent event) {
        println("Deployed " + event.getArtifact() + " to " + event.getRepository())
    }

    void artifactDeploying(RepositoryEvent event) {
        println("Deploying " + event.getArtifact() + " to " + event.getRepository())
    }

    void artifactDescriptorInvalid(RepositoryEvent event) {
        println("Invalid artifact descriptor for " + event.getArtifact() + ": "
                + event.getException().getMessage())
    }

    void artifactDescriptorMissing(RepositoryEvent event) {
        println("Missing artifact descriptor for " + event.getArtifact())
    }

    void artifactInstalled(RepositoryEvent event) {
        println("Installed " + event.getArtifact() + " to " + event.getFile())
    }

    void artifactInstalling(RepositoryEvent event) {
        println("Installing " + event.getArtifact() + " to " + event.getFile())
    }

    void artifactResolved(RepositoryEvent event) {
        println("Resolved artifact " + event.getArtifact() + " from " + event.getRepository())
    }

    void artifactDownloading(RepositoryEvent event) {
        println("Downloading artifact " + event.getArtifact() + " from " + event.getRepository())
    }

    void artifactDownloaded(RepositoryEvent event) {
        println("Downloaded artifact " + event.getArtifact() + " from " + event.getRepository())
    }

    void artifactResolving(RepositoryEvent event) {
        println("Resolving artifact " + event.getArtifact())
    }

    void metadataDeployed(RepositoryEvent event) {
        println("Deployed " + event.getMetadata() + " to " + event.getRepository())
    }

    void metadataDeploying(RepositoryEvent event) {
        println("Deploying " + event.getMetadata() + " to " + event.getRepository())
    }

    void metadataInstalled(RepositoryEvent event) {
        println("Installed " + event.getMetadata() + " to " + event.getFile())
    }

    void metadataInstalling(RepositoryEvent event) {
        println("Installing " + event.getMetadata() + " to " + event.getFile())
    }

    void metadataInvalid(RepositoryEvent event) {
        println("Invalid metadata " + event.getMetadata())
    }

    void metadataResolved(RepositoryEvent event) {
        println("Resolved metadata " + event.getMetadata() + " from " + event.getRepository())
    }

    void metadataResolving(RepositoryEvent event) {
        println("Resolving metadata " + event.getMetadata() + " from " + event.getRepository())
    }

}
