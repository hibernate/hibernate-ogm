/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.queryable.spi.BackendQuery;
import org.hibernate.ogm.dialect.queryable.spi.ClosableIterator;
import org.hibernate.ogm.dialect.queryable.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.dialect.queryable.spi.QueryableGridDialect;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.util.impl.Contracts;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;

/**
 * A {@link GridDialect} which delegates all the operations to another dialect implementation. Useful as base class in
 * case only a few methods of the {@code GridDialect} contract should be overridden, while delegating all the others.
 * <p>
 * <b>Note:</b> This class implements all the dialect facet interfaces such as {@link QueryableGridDialect}, one
 * therefore must not use {@code instanceof} to determine the capabilities of a dialect, but rather
 * {@link #isQueryable()} etc. should be used.
 *
 * @author Gunnar Morling
 */
public class ForwardingGridDialect<T extends Serializable> implements GridDialect, BatchableGridDialect, QueryableGridDialect<T>, Configurable, ServiceRegistryAwareService {

	private final GridDialect gridDialect;
	private final BatchableGridDialect batchableGridDialect;
	private final QueryableGridDialect<T> queryableGridDialect;

	@SuppressWarnings("unchecked")
	public ForwardingGridDialect(GridDialect gridDialect) {
		Contracts.assertParameterNotNull( gridDialect, "gridDialect" );

		this.gridDialect = gridDialect;
		this.batchableGridDialect = asBatchableGridDialectOrNull( gridDialect );
		this.queryableGridDialect = (QueryableGridDialect<T>) asQueryableGridDialectOrNull( gridDialect );
	}

	private static QueryableGridDialect<?> asQueryableGridDialectOrNull(GridDialect gridDialect) {
		if ( gridDialect instanceof ForwardingGridDialect ) {
			if ( ( (ForwardingGridDialect<?>) gridDialect ).isQueryable() ) {
				return (QueryableGridDialect<?>) gridDialect;
			}
		}
		else if ( gridDialect instanceof QueryableGridDialect ) {
			return (QueryableGridDialect<?>) gridDialect;
		}

		return null;
	}

	private static BatchableGridDialect asBatchableGridDialectOrNull(GridDialect gridDialect) {
		if ( gridDialect instanceof ForwardingGridDialect ) {
			if ( ( (ForwardingGridDialect<?>) gridDialect ).isBatchable() ) {
				return (BatchableGridDialect) gridDialect;
			}
		}
		else if ( gridDialect instanceof BatchableGridDialect ) {
			return (BatchableGridDialect) gridDialect;
		}

		return null;
	}

	/**
	 * Whether the wrapped dialect implementation implements the {@link QueryableGridDialect} facet or not.
	 */
	public boolean isQueryable() {
		return queryableGridDialect != null;
	}

	/**
	 * Whether the wrapped dialect implementation implements the {@link BatchableGridDialect} facet or not.
	 */
	public boolean isBatchable() {
		return batchableGridDialect != null;
	}

	/**
	 * Returns the wrapped dialect implementation.
	 */
	public GridDialect getGridDialect() {
		return gridDialect;
	}

	// GridDialect

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		return gridDialect.getLockingStrategy( lockable, lockMode );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		return gridDialect.getTuple( key, tupleContext );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		return gridDialect.createTuple( key, tupleContext );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		gridDialect.updateTuple( tuple, key, tupleContext );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		gridDialect.removeTuple( key, tupleContext );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		return gridDialect.getAssociation( key, associationContext );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		return gridDialect.createAssociation( key, associationContext );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext) {
		gridDialect.updateAssociation( association, key, associationContext );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		gridDialect.removeAssociation( key, associationContext );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return gridDialect.createTupleAssociation( associationKey, rowKey );
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
		return gridDialect.isStoredInEntityStructure( associationKey, associationContext );
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return gridDialect.nextValue( request );
	}

	@Override
	public boolean supportsSequences() {
		return gridDialect.supportsSequences();
	}

	@Override
	public GridType overrideType(Type type) {
		return gridDialect.overrideType( type );
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		gridDialect.forEachTuple( consumer, entityKeyMetadatas );
	}

	// BatchableGridDialect

	@Override
	public void executeBatch(OperationsQueue queue) {
		batchableGridDialect.executeBatch( queue );
	}

	// QueryableGridDialect

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<T> query, QueryParameters queryParameters) {
		return queryableGridDialect.executeBackendQuery( query, queryParameters );
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return queryableGridDialect.getParameterMetadataBuilder();
	}

	@Override
	public T parseNativeQuery(String nativeQuery) {
		return queryableGridDialect.parseNativeQuery( nativeQuery );
	}

	// ServiceRegistryAwareService

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		if ( gridDialect instanceof ServiceRegistryAwareService ) {
			( (ServiceRegistryAwareService) gridDialect ).injectServices( serviceRegistry );
		}
	}

	// Configurable

	@Override
	public void configure(Map configurationValues) {
		if ( gridDialect instanceof Configurable ) {
			( (Configurable) gridDialect ).configure( configurationValues );
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() );
		sb.append( " -> " );

		GridDialect delegate = gridDialect;
		while ( delegate instanceof ForwardingGridDialect ) {
			sb.append( delegate.getClass().getSimpleName() );
			sb.append( " -> " );
			delegate = ( (ForwardingGridDialect<?>) delegate ).getGridDialect();
		}

		sb.append( delegate.getClass().getSimpleName() );
		sb.append( " [isQueryable=" ).append( isQueryable() );
		sb.append( ", isBatchable=" ).append( isBatchable() ).append( "]" );

		return sb.toString();
	}
}
