/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import java.util.Map;

import org.hibernate.ogm.model.spi.Tuple;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;

/**
 * Iterates over the result of a native query when each result is a neo4j node.
 * This is the case when the result of native query is mapped by an entity type.
 *
 * @author Davide D'Alto
 */
public class NodesTupleIterator extends MapsTupleIterator {

	public NodesTupleIterator(ExecutionResult result) {
		super( result );
	}

	protected Tuple convert(Map<String, Object> next) {
		return createTuple( (Node) next.values().iterator().next() );
	}

	private Tuple createTuple(Node node) {
		return new Tuple( new Neo4jTupleSnapshot( node ) );
	}
}
