package com.github.wrm.pact.git.auth;

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public interface GitAuthenticationProvider {

    /**
     * Provides {@link UsernamePasswordCredentialsProvider} instance based on
     * userName and password.
     * @param userName 
     * @param password
     * @param secret
     */
    UsernamePasswordCredentialsProvider getCredentialProvider(String userName, String password);
    
}
