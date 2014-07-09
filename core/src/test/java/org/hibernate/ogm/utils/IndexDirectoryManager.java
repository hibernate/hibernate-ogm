/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.io.File;
import java.net.URL;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;


/**
 * Collects static constants used across several tests.
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
public class IndexDirectoryManager {

	private static final Log log = LoggerFactory.make();

	/**
	 * Returns the target directory of the build.
	 *
	 * @param testClass the test class for which the target directory is requested.
	 * @return the target directory of the build
	 */
	public static File getTargetDir(Class<?> testClass) {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		// get a URL reference to something we now is part of the classpath (our own classes)
		String currentTestClass = testClass.getName();
		int hopsToCompileDirectory = currentTestClass.split( "\\." ).length;
		int hopsToTargetDirectory = hopsToCompileDirectory + 1;
		URL classURL = contextClassLoader.getResource( currentTestClass.replace( '.', '/' ) + ".class" );
		// navigate back to '/target'
		File targetDir = new File( classURL.getFile() );
		// navigate back to '/target'
		for ( int i = 0; i < hopsToTargetDirectory; i++ ) {
			targetDir = targetDir.getParentFile();
		}
		return targetDir;
	}

	/**
	 * Return the root directory to store test indexes in. Tests should never use or delete this directly
	 * but rather nest sub directories in it to avoid interferences across tests.
	 *
	 * @param testClass the test class for which the index directory is requested.
	 * @return Return the root directory to store test indexes
	 */
	public static String getIndexDirectory(Class<?> testClass) {
		File targetDir = getTargetDir( testClass );
		String indexDirPath = targetDir.getAbsolutePath() + File.separator + "indextemp";
		log.debugf( "Using %s as index directory.", indexDirPath );
		return indexDirPath;
	}
}
