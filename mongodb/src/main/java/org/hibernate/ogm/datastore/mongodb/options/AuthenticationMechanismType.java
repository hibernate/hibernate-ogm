/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options;

import com.mongodb.MongoCredential;

/**
 * Available authentication methods for MongoDB.
 *
 * @see com.mongodb.MongoCredential
 * @author Davide D'Alto
 */
public enum AuthenticationMechanismType {

	GSSAPI {

		public MongoCredential createCredential(String username, String databaseName, String password) {
			return MongoCredential.createGSSAPICredential( username );
		}
	},
	MONGODB_CR {

		public MongoCredential createCredential(String username, String databaseName, String password) {
			return MongoCredential.createMongoCRCredential( username, databaseName, asCharArray( password ) );
		}
	},
	PLAIN {

		public MongoCredential createCredential(String username, String databaseName, String password) {
			return MongoCredential.createPlainCredential( username, databaseName, asCharArray( password ) );
		}
	},
	MONGODB_X509 {

		public MongoCredential createCredential(String username, String databaseName, String password) {
			return MongoCredential.createMongoX509Credential( username );
		}
	};

	private static char[] asCharArray(String password) {
		if ( password == null ) {
			return null;
		}
		return password.toCharArray();
	}

	public abstract MongoCredential createCredential(String username, String databaseName, String password);
}
