/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

/**
 * A type {@link Neo4jTypeConverter} for embedded Neo4j.
 *
 * @author Davide D'Alto
 */
public class EmbeddedNeo4jTypeConverter extends Neo4jTypeConverter {

	public static final EmbeddedNeo4jTypeConverter INSTANCE = new EmbeddedNeo4jTypeConverter();

	private EmbeddedNeo4jTypeConverter() {
	}
}
