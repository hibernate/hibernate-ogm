/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j;

import org.hibernate.ogm.datastore.neo4j.embedded.impl.EmbeddedNeo4jDatastoreProvider;

/**
 * Equivalent to {@link EmbeddedNeo4jDialect}.
 *
 * @deprecated Use {@link EmbeddedNeo4jDialect} instead
 * @author Davide D'Alto
 */
@Deprecated
public class Neo4jDialect extends EmbeddedNeo4jDialect {

	public Neo4jDialect(EmbeddedNeo4jDatastoreProvider provider) {
		super( provider );
	}
}
