/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.EntityMetadataInformation;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

/**
 * Iterator over the result of a backend query.
 * <p>
 * If {@link EntityKeyMetadata} is not {@code null}, it expects the result to contain a list of {@link Node}.
 *
 * @author Davide D'Alto
 */
public class EmbeddedNeo4jBackendQueryResultIterator extends EmbeddedNeo4jTupleIterator<Map<String, Object>> {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final EntityKeyMetadata entityKeyMetadata;
	private final TupleTypeContext tupleTypeContext;
	private final List<String> columns;

	public EmbeddedNeo4jBackendQueryResultIterator(Result result, EntityMetadataInformation info, TupleContext tupleContext) {
		super( result );
		this.columns = result != null ? result.columns() : null;
		this.entityKeyMetadata = info != null ? info.getEntityKeyMetadata() : null;
		this.tupleTypeContext = info != null ? tupleContext.getTupleTypeContext() : null;
	}

	@Override
	protected Tuple convert(Map<String, Object> next) {
		TupleSnapshot snapshot = createSnapshot( next );
		return new Tuple( snapshot, SnapshotType.UPDATE );
	}

	private TupleSnapshot createSnapshot(Map<String, Object> next) {
		if ( entityKeyMetadata == null ) {
			return mapSnapshot( next );
		}

		Object value = next.values().iterator().next();
		if ( value instanceof Node ) {
			return nodeSnapshot( (Node) value );
		}

		// Projections and addEntities are not allowed in the same query at the same time
		throw log.addEntityNotAllowedInNativeQueriesUsingProjection( entityKeyMetadata.getTable(), "" );
	}

	private TupleSnapshot mapSnapshot(Map<String, Object> next) {
		TupleSnapshot snapshot;
		if ( this.columns != null ) {
			Map<String, Object> sortedColumns = new LinkedHashMap<>();
			for ( String column : this.columns ) {
				sortedColumns.put( column, next.get( column ) );
			}
			next = sortedColumns;
		}
		snapshot = new MapTupleSnapshot( (Map<String, Object>) next );
		return snapshot;
	}

	private TupleSnapshot nodeSnapshot(Node node) {
		return EmbeddedNeo4jTupleSnapshot.fromNode(
				node,
				tupleTypeContext.getAllAssociatedEntityKeyMetadata(),
				tupleTypeContext.getAllRoles(),
				entityKeyMetadata );
	}
}
