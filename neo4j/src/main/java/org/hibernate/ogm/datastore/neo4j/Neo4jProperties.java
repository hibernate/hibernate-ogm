/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j;

import org.hibernate.ogm.cfg.OgmProperties;

/**
 * Neo4j GridDialect configuration options.
 *
 * @author Davide D'Alto
 * @see org.hibernate.ogm.datastore.neo4j.Neo4jDialect
 */
public final class Neo4jProperties implements OgmProperties {

	/**
	 * The absolute path representing the location of the Neo4j database, ex.: /home/user/neo4jdb/mydb
	 */
	public static final String DATABASE_PATH = "hibernate.ogm.neo4j.database_path";

	/**
	 * Location of the Neo4j embedded properties file. It can be the name of a class path resource, an URL or an
	 * absolute file path.
	 */
	public static final String CONFIGURATION_RESOURCE_NAME = "hibernate.ogm.neo4j.configuration_resource_name";

	/**
	 * Name of the neo4j index containing the stored entities. Default to {@link #DEFAULT_NEO4J_ENTITY_INDEX_NAME}
	 */
	public static final String ENTITY_INDEX_NAME = "hibernate.ogm.neo4j.index.entity";

	/**
	 * Name of the Neo4j index containing the stored associations. Default to
	 * {@link #DEFAULT_NEO4J_ASSOCIATION_INDEX_NAME}
	 */
	public static final String ASSOCIATION_INDEX_NAME = "hibernate.ogm.neo4j.index.association";

	/**
	 * Name of the index that stores the next available value for a sequence. Default to
	 * {@link #DEFAULT_NEO4J_SEQUENCE_INDEX_NAME}
	 */
	public static final String SEQUENCE_INDEX_NAME = "hibernate.ogm.neo4j.index.sequence";

	private Neo4jProperties() {
	}
}
