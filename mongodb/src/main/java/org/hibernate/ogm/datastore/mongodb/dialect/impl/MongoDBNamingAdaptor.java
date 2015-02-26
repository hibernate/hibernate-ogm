/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.spi.DialectNamingAdaptor;
import org.hibernate.ogm.util.impl.StringHelper;

public final class MongoDBNamingAdaptor implements DialectNamingAdaptor {

	public static final DialectNamingAdaptor INSTANCE = new MongoDBNamingAdaptor();

	private static final Log log = LoggerFactory.getLogger();

	private MongoDBNamingAdaptor() {
		//use the singleton INSTANCE
	}

	@Override
	public String makeValidTableName(String requestedName) {
		if ( StringHelper.isEmpty( requestedName ) ) {
			throw new org.hibernate.AssertionFailure( "qualifiedName is not expected to be null at this point" );
		}
		else if ( requestedName.startsWith( "system." ) ) {
			throw log.collectionNameHasInvalidSystemPrefix( requestedName );
		}
		else if ( requestedName.contains( "\u0000" ) ) {
			throw log.collectionNameContainsNULCharacter( requestedName );
		}
		else if ( requestedName.contains( "$" ) ) {
			throw log.collectionNameContainsDollarCharacter( requestedName );
		}
		return requestedName;
	}

}
