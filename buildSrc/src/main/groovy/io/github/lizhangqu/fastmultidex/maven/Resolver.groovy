package io.github.lizhangqu.fastmultidex.maven

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.deployment.DeployRequest
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.repository.AuthenticationBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project;


class Resolver {
    private Project project
    private RemoteRepository resolverRepository
    private RemoteRepository uploadRepository
    private LocalRepository localRepository
    private RepositorySystem repositorySystem


    Resolver(Project project, RemoteRepository resolverRepository, RemoteRepository uploadRepository, LocalRepository localRepository) {
        this.project = project
        this.resolverRepository = resolverRepository
        this.uploadRepository = uploadRepository
        this.localRepository = localRepository
        this.repositorySystem = newRepositorySystem()
    }

    Resolver(Project project, String resolverUrl, File baseDir, String uploadUrl, String username, String password) {
        this.project = project
        this.resolverRepository = newResolverRepository(resolverUrl)
        this.uploadRepository = newUploadRepository(uploadUrl, username, password)
        this.localRepository = newLocalRepository(baseDir)
        this.repositorySystem = newRepositorySystem()
    }


    Resolver(Project project, boolean snapshot) {
        this.project = project
        Properties properties = loadLocalProperties(project)
        this.resolverRepository = newResolverRepository(getRepositoryUrl(project, properties))
        if (snapshot) {
            this.uploadRepository = newUploadRepository(getSnapshotRepositoryUrl(project, properties), getSnapshotRepositoryUsername(project, properties), getSnapshotRepositoryPassword(project, properties))
        } else {
            this.uploadRepository = newUploadRepository(getReleaseRepositoryUrl(project, properties), getReleaseRepositoryUsername(project, properties), getReleaseRepositoryPassword(project, properties))
        }
        this.localRepository = newLocalRepository(new File(System.getProperty("user.home"), ".fastmultidex/.m2"))
        this.repositorySystem = newRepositorySystem()
    }

    Resolver(Project project) {
        this(project, true)
    }

    private static Properties loadLocalProperties(Project project) {
        Properties properties = new Properties()
        try {
            File localFile = project.rootProject.file('local.properties')
            if (localFile.exists()) {
                properties.load(localFile.newDataInputStream())
            }
        } catch (Exception e) {
            println("load local properties failed msg:${e.message}")
        }
        return properties
    }

    private static
    def readPropertyFromLocalProperties(Project project, Properties properties, String key, String defaultValue) {
        readPropertyFromLocalPropertiesOrThrow(project, properties, key, defaultValue, false)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    private static
    def readPropertyFromLocalPropertiesOrThrow(Project project, Properties properties, String key, String defaultValue, boolean throwIfNull) {
        def property = (properties != null && properties.containsKey(key)) ? properties.getProperty(key, defaultValue) : defaultValue
        if (property == null && throwIfNull) {
            throw new GradleException("you must config ${key} in properties. Like config project.ext.${key} , add ${key} in gradle.properties or add ${key} in local.properties which locates on root project dir")
        }
        return property
    }

    private static def getRepositoryUrl(Project project, Properties properties) {
        return project.hasProperty('REPOSITORY_URL') ? project.ext.REPOSITORY_URL : readPropertyFromLocalProperties(project, properties, 'REPOSITORY_URL', null)
    }

    private static def getReleaseRepositoryUrl(Project project, Properties properties) {
        return project.hasProperty('RELEASE_REPOSITORY_URL') ? project.ext.RELEASE_REPOSITORY_URL : readPropertyFromLocalProperties(project, properties, 'RELEASE_REPOSITORY_URL', null)
    }

    private static def getSnapshotRepositoryUrl(Project project, Properties properties) {
        return project.hasProperty('SNAPSHOT_REPOSITORY_URL') ? project.ext.SNAPSHOT_REPOSITORY_URL : readPropertyFromLocalProperties(project, properties, 'SNAPSHOT_REPOSITORY_URL', null)
    }

    private static def getReleaseRepositoryUsername(Project project, Properties properties) {
        return project.hasProperty('RELEASE_REPOSITORY_USERNAME') ? project.ext.RELEASE_REPOSITORY_USERNAME : readPropertyFromLocalProperties(project, properties, 'RELEASE_REPOSITORY_USERNAME', null)
    }

    private static def getReleaseRepositoryPassword(Project project, Properties properties) {
        return project.hasProperty('RELEASE_REPOSITORY_PASSWORD') ? project.ext.RELEASE_REPOSITORY_PASSWORD : readPropertyFromLocalProperties(project, properties, 'RELEASE_REPOSITORY_PASSWORD', null)
    }

    private static def getSnapshotRepositoryUsername(Project project, Properties properties) {
        return project.hasProperty('SNAPSHOT_REPOSITORY_USERNAME') ? project.ext.SNAPSHOT_REPOSITORY_USERNAME : readPropertyFromLocalProperties(project, properties, 'SNAPSHOT_REPOSITORY_USERNAME', null)
    }

    private static def getSnapshotRepositoryPassword(Project project, Properties properties) {
        return project.hasProperty('SNAPSHOT_REPOSITORY_PASSWORD') ? project.ext.SNAPSHOT_REPOSITORY_PASSWORD : readPropertyFromLocalProperties(project, properties, 'SNAPSHOT_REPOSITORY_PASSWORD', null)
    }

    private static LocalRepository newLocalRepository(File baseDir) {
        return new LocalRepository(baseDir);
    }

    private static RemoteRepository newResolverRepository(String url) {
        return new RemoteRepository.Builder("nexus", "default", url).build()
    }

    private
    static RemoteRepository newUploadRepository(String url, String username, String password) {
        Authentication authentication = new AuthenticationBuilder().addUsername(username).addPassword(password).build()
        RemoteRepository nexus =
                new RemoteRepository.Builder("nexus", "default", url).setAuthentication(authentication).build()
        return nexus
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class)
        locator.addService(TransporterFactory.class, FileTransporterFactory.class)
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class)

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                exception.printStackTrace()
            }
        })

        return locator.getService(RepositorySystem.class)
    }

    private DefaultRepositorySystemSession newRepositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession()
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository))
        session.setTransferListener(new ConsoleTransferListener(project))
        session.setRepositoryListener(new ConsoleRepositoryListener(project))
        return session
    }

    Artifact resolve(String groupId, String artifactId, String version) {
        try {
            RepositorySystemSession session = newRepositorySystemSession()
            Artifact artifact = new DefaultArtifact(groupId, artifactId, "jar", version)

            ArtifactRequest artifactRequest = new ArtifactRequest()
            artifactRequest.setArtifact(artifact)
            artifactRequest.addRepository(resolverRepository)

            ArtifactResult artifactResult = repositorySystem.resolveArtifact(session, artifactRequest)

            artifact = artifactResult.getArtifact()
            return artifact
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    void install(String groupId, String artifactId, String version, File srcFile)
            throws InstallationException {
        try {
            RepositorySystemSession session = newRepositorySystemSession();

            Artifact jarArtifact = new DefaultArtifact(groupId, artifactId, "jar", version)
            jarArtifact = jarArtifact.setFile(srcFile)

            InstallRequest installRequest = new InstallRequest()
            installRequest.addArtifact(jarArtifact)
            repositorySystem.install(session, installRequest)
        } catch (Exception e) {
            e.printStackTrace()
        }

    }

    void deploy(String groupId, String artifactId, String version, File srcFile) {
        try {
            RepositorySystemSession session = newRepositorySystemSession();

            Artifact jarArtifact = new DefaultArtifact(groupId, artifactId, "jar", version)
            jarArtifact = jarArtifact.setFile(srcFile)

            DeployRequest deployRequest = new DeployRequest()
            deployRequest.addArtifact(jarArtifact)
            deployRequest.setRepository(uploadRepository)

            repositorySystem.deploy(session, deployRequest)
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}