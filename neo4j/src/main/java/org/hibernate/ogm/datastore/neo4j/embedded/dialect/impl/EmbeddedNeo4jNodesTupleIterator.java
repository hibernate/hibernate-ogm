/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl;

import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;

/**
 * @author Davide D'Alto
 */
public class EmbeddedNeo4jNodesTupleIterator extends EmbeddedNeo4jTupleIterator<Node> {

	private final EntityKeyMetadata entityKeyMetadata;
	private final TupleTypeContext tupleTypeContext;

	public EmbeddedNeo4jNodesTupleIterator(ResourceIterator<Node> result, EntityKeyMetadata entityKeyMetadata, TupleTypeContext tupleTypeContext) {
		super( result );
		this.entityKeyMetadata = entityKeyMetadata;
		this.tupleTypeContext = tupleTypeContext;
	}

	@Override
	protected Tuple convert(Node node) {
		return new Tuple( EmbeddedNeo4jTupleSnapshot.fromNode( node,
				tupleTypeContext.getAllAssociatedEntityKeyMetadata(), tupleTypeContext.getAllRoles(),
				entityKeyMetadata ), SnapshotType.UPDATE );
	}
}
