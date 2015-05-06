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
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public enum AuthenticationMechanismType {

	GSSAPI {

		public MongoCredential createCredential(String username, String databaseName, String password) {
			return MongoCredential.createGSSAPICredential( username );
		}
	},
	/**
	 * @deprecated since MongoDB 3.0, use {@link #SCRAM_SHA_1} or {@link #BEST}
	 */
	@Deprecated
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
	}
	,
	SCRAM_SHA_1 {

		public MongoCredential createCredential(String username, String databaseName, String password) {
			return MongoCredential.createScramSha1Credential( username, databaseName, asCharArray( password ) );
		}
	},
	/**
	 * The client will negotiate the best mechanism based on
	 * the version of the server that the client is authenticating to.
	 */
	BEST {

		public MongoCredential createCredential(String username, String databaseName, String password) {
			return MongoCredential.createCredential( username, databaseName, asCharArray( password ) );
		}
	}
	;

	private static char[] asCharArray(String password) {
		if ( password == null ) {
			return null;
		}
		return password.toCharArray();
	}

	public abstract MongoCredential createCredential(String username, String databaseName, String password);
}
