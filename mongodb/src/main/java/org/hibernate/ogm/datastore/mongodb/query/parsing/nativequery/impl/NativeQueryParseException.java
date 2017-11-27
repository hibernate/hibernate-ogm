/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl;

/**
 *  An exception thrown by {@link MongoDBQueryDescriptorBuilder}
 *  if the input is not correct.
 */
public class NativeQueryParseException extends RuntimeException {
	public NativeQueryParseException(String message) {
		super( message );
	}

	public NativeQueryParseException(Exception e) {
		super( e );
	}
}
