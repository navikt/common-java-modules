package com.github.wrm.pact.git;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.transport.CredentialsProvider;

public class GitApi {

	Git repository;
	Optional<CredentialsProvider> credentialsProvider;

	public GitApi() {

	}

	public void initWithCredentials(File repoDir, String url, Optional<CredentialsProvider> credentialsProvider)
			throws Exception {
		this.credentialsProvider = credentialsProvider;
		try {
			repository = Git.open(repoDir);
			PullCommand pullCmd = repository.pull();
			credentialsProvider.ifPresent(c -> pullCmd.setCredentialsProvider(c));
			pullCmd.call();
		} catch (IOException ex) {
			// failed to open, so we clone it anew
			CloneCommand cloneCmd = Git.cloneRepository();
			credentialsProvider.ifPresent(c -> cloneCmd.setCredentialsProvider(c));
			repository = cloneCmd.setDirectory(repoDir).setURI(url).call();
		}
	}

	/**
	 * adds, commits and pushes changes only, if there are actually changes
	 * 
	 * @param message
	 * @return false, if there were no changes to be pushed
	 * @throws Exception
	 */
	public boolean pushChanges(String message) throws Exception {

		if (repository.diff().call().isEmpty()) {
			return false;
		}

		repository.add().addFilepattern(".").call();
		repository.commit().setMessage(message).call();

		PushCommand pushCmd = repository.push();
		credentialsProvider.ifPresent(c -> pushCmd.setCredentialsProvider(c));
		pushCmd.call();
		return true;
	}

}
