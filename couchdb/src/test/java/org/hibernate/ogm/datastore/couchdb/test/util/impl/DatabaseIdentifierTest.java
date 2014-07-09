/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.test.util.impl;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.datastore.couchdb.util.impl.DatabaseIdentifier;
import org.junit.Test;

/**
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 */
public class DatabaseIdentifierTest {

	@Test
	public void shouldReturnTheCorrectServerUri() throws Exception {
		String expectedServerUri = "http://localhost:5984";
		DatabaseIdentifier databaseIdentifier = new DatabaseIdentifier( "localhost", 5984, "databasename", "", "" );

		assertThat( databaseIdentifier.getServerUri().toString() ).isEqualTo( expectedServerUri );
	}

	@Test
	public void shouldReturnTheCorrectDatabaseName() throws Exception {
		String expectedName = "not_important";
		DatabaseIdentifier databaseIdentifier = new DatabaseIdentifier( "localhost", 5984, expectedName, "", "" );

		assertThat( databaseIdentifier.getDatabaseName() ).isEqualTo( expectedName );
	}
}
