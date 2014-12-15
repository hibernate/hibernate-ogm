/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.io.File;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * @author Davide D'Alto
 */
public class FileHelper {

	private static final Log logger = LoggerFactory.make();

	/**
	 * Attempts to delete a file. If the file is a directory delete recursively all content.
	 *
	 * @param file the file or directory to be deleted
	 * @return {@code false} if it wasn't possible to delete all content which is a common problem on Windows systems.
	 */
	public static boolean delete(File file) {
		if ( file == null ) {
			throw new IllegalArgumentException();
		}
		boolean allok = true;
		if ( file.isDirectory() ) {
			for ( File subFile : file.listFiles() ) {
				boolean deleted = delete( subFile );
				allok = allok && deleted;
			}
		}
		if ( allok && file.exists() ) {
			if ( !file.delete() ) {
				logger.warnf( "File not deleted: %1", file );
				return false;
			}
		}
		return allok;
	}

}
