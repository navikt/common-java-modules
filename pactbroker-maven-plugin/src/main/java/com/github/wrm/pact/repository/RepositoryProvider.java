package com.github.wrm.pact.repository;

import java.io.File;
import java.util.List;
import java.util.Optional;

import com.github.wrm.pact.domain.PactFile;

public interface RepositoryProvider {

	/**
	 * the provider uploads all given pacts to the given url and tags the uploaded version, if given.
	 * @param pacts
	 * @param tagName
	 * @throws Exception
	 */
	void uploadPacts(List<PactFile> pacts, Optional<String> tagName) throws Exception;

	
	
	/**
	 * the provider downloads all necessary pacts to the given repository
	 * @param providerId
	 * @param targetDirectory
	 * @throws Exception
	 */
	void downloadPacts(String providerId, String tagName, File targetDirectory) throws Exception;

}
