/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j;

import org.hibernate.ogm.dialect.spi.GridDialect;

/**
 * Flag a Neo4j {@link GridDialect} as remote.
 *
 * @author Davide D'Alto
 */
public interface RemoteNeo4jDialect extends GridDialect {

}
