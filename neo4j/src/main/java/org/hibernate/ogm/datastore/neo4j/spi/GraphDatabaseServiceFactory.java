/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.spi;

import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;

/**
 * Contains methods to create a {@link GraphDatabaseService}.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public interface GraphDatabaseServiceFactory {

	/**
	 * Called after the creation of the factory can be used to read the configuration.
	 *
	 * @param properties
	 *            configuration properties
	 */
	void initialize(Map<?, ?> properties);

	/**
	 * Creates a {@link GraphDatabaseService}.
	 *
	 * @return a new {@link GraphDatabaseService} instance
	 */
	GraphDatabaseService create();

}
