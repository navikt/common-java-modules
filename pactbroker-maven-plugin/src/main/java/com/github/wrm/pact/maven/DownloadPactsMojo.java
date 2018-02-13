package com.github.wrm.pact.maven;

import java.io.File;
import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.wrm.pact.repository.RepositoryProvider;

/**
 * Verifies all pacts that can be found for this provider
 */
@Mojo(name = "download-pacts")
@Execute(phase = LifecyclePhase.GENERATE_TEST_RESOURCES)
public class DownloadPactsMojo extends AbstractPactsMojo {

    /**
     * url of pact broker
     */
    @Parameter
    private String brokerUrl;

    /**
     * Consumer version
     */
    @Parameter(defaultValue = "1.0.0")
    private String consumerVersion;

    @Parameter
    private String tagName;
    /**
     * Location of pacts
     */
    @Parameter(defaultValue = "target/pacts-dependents")
    private String pacts;

    /**
     * Name of this provider
     */
    @Parameter
    private String provider;

    /**
     * username of git repository
     */
    @Parameter
    private String username;

    @Parameter(defaultValue = "false")
    private boolean insecure;

    /**
     * password of git repository
     */
    @Parameter
    private String password;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            RepositoryProvider repoProvider = createRepositoryProvider(brokerUrl, consumerVersion, Optional.ofNullable(username), Optional.ofNullable(password), insecure);
            repoProvider.downloadPacts(provider, tagName, new File(pacts));
        }
        catch (Throwable e) {
            throw new MojoExecutionException("Failed to download pacts", e);
        }
    }

}