/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamId;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamPayload;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.query.spi.RowSelection;
import org.hibernate.ogm.dialect.query.spi.TypedGridValue;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

/**
 * Handles the query execution on the infinispan server.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteQueryHandler {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final InfinispanRemoteDatastoreProvider provider;

	public InfinispanRemoteQueryHandler(InfinispanRemoteDatastoreProvider provider) {
		this.provider = provider;
	}

	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<InfinispanRemoteQueryDescriptor> backendQuery, QueryParameters queryParameters) {
		EntityKeyMetadata entityKeyMetadata = backendQuery.getSingleEntityMetadataInformationOrNull() == null
				? null
				: backendQuery.getSingleEntityMetadataInformationOrNull().getEntityKeyMetadata();

		InfinispanRemoteQueryDescriptor queryDescriptor = backendQuery.getQuery();
		RemoteCache<ProtostreamId, ProtostreamPayload> cache = provider.getCache( queryDescriptor.getCache() );

		QueryFactory queryFactory = Search.getQueryFactory( cache );
		Query query = queryFactory.create( queryDescriptor.getQuery() );

		applyNamedParameters( queryParameters, query );
		applyRowSelection( queryParameters, query );

		boolean hasProjection = hasProjection( queryDescriptor );
		if ( entityKeyMetadata != null && hasProjection ) {
			throw log.addEntityNotAllowedInNativeQueriesUsingProjection( entityKeyMetadata.getTable(), backendQuery.toString() );
		}

		return hasProjection
				? new RawTypeClosableIterator( query, queryDescriptor.getProjections() )
				: new ProtostreamPayloadClosableIterator( query.list() );
	}

	// We are using QueryDescriptor and not Query because the QueryFactory.create( ) doesn't initialize the projection field
	private boolean hasProjection(InfinispanRemoteQueryDescriptor queryDescriptor) {
		return queryDescriptor.getProjections() != null && queryDescriptor.getProjections().length > 0;
	}

	private void applyNamedParameters(QueryParameters queryParameters, Query query) {
		for ( Map.Entry<String, TypedGridValue> param : queryParameters.getNamedParameters().entrySet() ) {
			query.setParameter( param.getKey(), getValue( param ) );
		}
	}

	private Object getValue(Map.Entry<String, TypedGridValue> param) {
		Object value = param.getValue().getValue();

		// Protobuf does not support Character type
		// convert to String type
		if ( value instanceof Character ) {
			return value.toString();
		}

		return value;
	}

	private void applyRowSelection(QueryParameters queryParameters, Query query) {
		RowSelection rowSelection = queryParameters.getRowSelection();
		if ( rowSelection == null ) {
			return;
		}

		Integer firstRow = rowSelection.getFirstRow();
		Integer maxRows = rowSelection.getMaxRows();

		if ( firstRow != null ) {
			query.startOffset( firstRow );
		}
		if ( maxRows != null ) {
			query.maxResults( maxRows );
		}
	}
}
