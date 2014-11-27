/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.common.externalizer.impl;

import java.io.IOException;
import java.io.ObjectInput;

import org.hibernate.ogm.datastore.infinispan.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispan.logging.impl.LoggerFactory;

/**
 * Helps with checking the version of persisted keys.
 *
 * @author Gunnar Morling
 */
public class VersionChecker {

	private static final Log LOG = LoggerFactory.getLogger();

	/**
	 * Consumes the version field from the given input and raises an exception if the record is in a newer version,
	 * written by a newer version of Hibernate OGM.
	 *
	 * @param input the input to read from
	 * @param supportedVersion the type version supported by this version of OGM
	 * @param externalizedType the type to be unmarshalled
	 *
	 * @throws IOException if an error occurs while reading the input
	 */
	public static void readAndCheckVersion(ObjectInput input, int supportedVersion, Class<?> externalizedType) throws IOException {
		int version = input.readInt();

		if ( version != supportedVersion ) {
			throw LOG.unexpectedKeyVersion( externalizedType, version, supportedVersion );
		}
	}
}
