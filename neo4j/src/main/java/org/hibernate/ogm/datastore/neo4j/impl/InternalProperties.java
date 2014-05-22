/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.impl;

/**
 * Internal properties not intended to be set by the user.
 *
 * @author Gunnar Morling
 */
public class InternalProperties {

	/**
	 * Qualified class name for the creation of a new {@link org.neo4j.graphdb.GraphDatabaseService}.
	 * <p>
	 * The class must implement the interface
	 * {@link org.hibernate.ogm.datastore.neo4j.spi.GraphDatabaseServiceFactory}.
	 */
	public static final String NEO4J_GRAPHDB_FACTORYCLASS = "hibernate.ogm.neo4j.graphdb_factoryclass";

	private InternalProperties() {
	}
}
