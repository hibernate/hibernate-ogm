/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.utils;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
import org.hibernate.ogm.datastore.neo4j.index.impl.Neo4jIndexSpec;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;

/**
 * Groups together the operations needed by the {@link Neo4jTestHelper} that are different for each Neo4j backend.
 *
 * @author Davide D'Alto
 */
public interface Neo4jTestHelperDelegate {

	String INDEX_LABEL = "label";
	String INDEX_PROPERTIES = "properties";
	String INDEX_TYPE = "type";
	String INDEX_TYPE_UNIQUE = "node_unique_property";

	/**
	 * Calls procedure for getting the existing indexes in the db.
	 */
	String GET_DB_INDEXES_PROCEDURE = "CALL db.indexes;";

	String GET_DB_CONSTRAINTS_PROCEDURE = "CALL db.constraints;";

	/**
	 * Query for counting all entities. This takes embedded nodes into account.
	 */
	String ENTITY_COUNT_QUERY = "MATCH (n) WHERE n:" + NodeLabel.ENTITY.name() + " OR n:" + NodeLabel.EMBEDDED.name()
			+ " RETURN COUNT(n) as count";

	String ASSOCIATION_COUNT_QUERY = "MATCH (n) -[r]-> () WITH n as e, count(distinct(type(r))) as c RETURN count(*) as count";

	String DELETE_ALL = "MATCH (n) OPTIONAL MATCH (n) -[r]-> () DELETE n,r";

	String ESCAPE_CONSTRAINT_REGEX = "ASSERT (.*?)\\.(.*) IS UNIQUE";

	/* This is an hack to escape embedded columns
	 *
	 * Example:
	 * DROP CONSTRAINT ON ( account:ACCOUNT ) ASSERT account.id.id IS UNIQUE
	 *
	 * becomes
	 *
	 * DROP CONSTRAINT ON ( account:ACCOUNT ) ASSERT account.`id.id` IS UNIQUE
	 */
	default String dropUniqueConstraintQuery(String constraint) {
		return "DROP " + constraint.replaceAll( ESCAPE_CONSTRAINT_REGEX, "ASSERT $1.`$2` IS UNIQUE" );
	}

	long getNumberOfEntities(Session session, DatastoreProvider provider);

	long getNumberOfAssociations(Session session, DatastoreProvider provider);

	List<Neo4jIndexSpec> getIndexes(Session sesssion, DatastoreProvider provider);

	GridDialect getDialect(DatastoreProvider datastoreProvider);

	void deleteAllElements(DatastoreProvider datastoreProvider);

	void dropDatabase(DatastoreProvider provider);

	Long executeCountQuery(DatastoreProvider datastoreProvider, String queryString);

	void executeCypherQuery(DatastoreProvider datastoreProvider, String query, Map<String, Object> parameters);

	Object findNode(DatastoreProvider datastoreProvider, NodeForGraphAssertions node);

	Map<String, Object> findProperties(DatastoreProvider datastoreProvider, NodeForGraphAssertions node);

	Object findRelationshipStartNode(DatastoreProvider datastoreProvider, RelationshipsChainForGraphAssertions relationship);
}
