package com.github.wrm.pact.git.auth;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * Provides {@link CredentialsProvider}
 *
 */
public class BasicGitCredentialsProvider implements GitAuthenticationProvider {
    @Override
    public UsernamePasswordCredentialsProvider getCredentialProvider(String userName, String password) {
        UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(userName, password);
        return credentialsProvider;
    }
    
}
