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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.identity.spi.IdentityColumnAwareGridDialect;
import org.hibernate.ogm.dialect.optimisticlock.spi.OptimisticLockingAwareGridDialect;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.SessionFactoryLifecycleAwareDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
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
 * A {@link GridDialect} which delegates all the operations to another dialect implementation. Useful as base class for
 * dialect wrappers which wish to override only a few methods of the {@code GridDialect}, while delegating all the
 * others.
 * <p>
 * <b>Note:</b> This class implements all the dialect facet interfaces such as {@link QueryableGridDialect}, one
 * therefore must not use {@code instanceof} to determine the capabilities of a dialect, but rather
 * {@link GridDialects#getDialectFacetOrNull(GridDialect, Class)} should be used.
 *
 * @author Gunnar Morling
 */
public class ForwardingGridDialect<T extends Serializable> implements GridDialect, BatchableGridDialect, SessionFactoryLifecycleAwareDialect, IdentityColumnAwareGridDialect, QueryableGridDialect<T>, OptimisticLockingAwareGridDialect, Configurable, ServiceRegistryAwareService {

	private final GridDialect gridDialect;
	private final BatchableGridDialect batchableGridDialect;
	private final QueryableGridDialect<T> queryableGridDialect;
	private final SessionFactoryLifecycleAwareDialect sessionFactoryAwareDialect;
	private final IdentityColumnAwareGridDialect identityColumnAwareGridDialect;
	private final OptimisticLockingAwareGridDialect optimisticLockingAwareGridDialect;

	@SuppressWarnings("unchecked")
	public ForwardingGridDialect(GridDialect gridDialect) {
		Contracts.assertParameterNotNull( gridDialect, "gridDialect" );

		this.gridDialect = gridDialect;
		this.batchableGridDialect = GridDialects.getDialectFacetOrNull( gridDialect, BatchableGridDialect.class );
		this.queryableGridDialect = GridDialects.getDialectFacetOrNull( gridDialect, QueryableGridDialect.class );
		this.sessionFactoryAwareDialect = GridDialects.getDialectFacetOrNull( gridDialect, SessionFactoryLifecycleAwareDialect.class );
		this.identityColumnAwareGridDialect = GridDialects.getDialectFacetOrNull( gridDialect, IdentityColumnAwareGridDialect.class );
		this.optimisticLockingAwareGridDialect = GridDialects.getDialectFacetOrNull( gridDialect, OptimisticLockingAwareGridDialect.class );
	}

	/**
	 * Returns the wrapped dialect implementation.
	 */
	public GridDialect getGridDialect() {
		return gridDialect;
	}

	/*
	 * @see org.hibernate.ogm.dialect.spi.GridDialect
	 */

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
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		gridDialect.insertOrUpdateTuple( key, tuple, tupleContext );
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
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		gridDialect.insertOrUpdateAssociation( key, association, associationContext );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		gridDialect.removeAssociation( key, associationContext );
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return gridDialect.isStoredInEntityStructure( associationKeyMetadata, associationTypeContext );
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

	/*
	 * @see org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect
	 */

	@Override
	public void executeBatch(OperationsQueue queue) {
		batchableGridDialect.executeBatch( queue );
	}

	/*
	 * @see org.hibernate.ogm.dialect.queryable.spi.QueryableGridDialect
	 */

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

	/*
	 * @see org.hibernate.ogm.dialect.optimisticlock.spi.OptimisticLockingAwareGridDialect
	 */

	@Override
	public boolean updateTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, Tuple tuple, TupleContext tupleContext) {
		return optimisticLockingAwareGridDialect.updateTupleWithOptimisticLock( entityKey, oldLockState, tuple, tupleContext );
	}

	@Override
	public boolean removeTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, TupleContext tupleContext) {
		return optimisticLockingAwareGridDialect.removeTupleWithOptimisticLock( entityKey, oldLockState, tupleContext );
	}

	/*
	 * @see org.hibernate.ogm.dialect.spi.SessionFactoryLifecycleAwareDialect
	 */

	@Override
	public void sessionFactoryCreated(SessionFactoryImplementor sessionFactoryImplementor) {
		sessionFactoryAwareDialect.sessionFactoryCreated( sessionFactoryImplementor );
	}

	/*
	 * @see org.hibernate.ogm.dialect.identitycolumnaware.IdentityColumnAwareGridDialect
	 */

	@Override
	public Tuple createTuple(EntityKeyMetadata entityKeyMetadata, TupleContext tupleContext) {
		return identityColumnAwareGridDialect.createTuple( entityKeyMetadata, tupleContext );
	}

	@Override
	public void insertTuple(EntityKeyMetadata entityKeyMetadata, Tuple tuple, TupleContext tupleContext) {
		identityColumnAwareGridDialect.insertTuple( entityKeyMetadata, tuple, tupleContext );
	}

	/*
	 * @see org.hibernate.service.spi.ServiceRegistryAwareService
	 */

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		if ( gridDialect instanceof ServiceRegistryAwareService ) {
			( (ServiceRegistryAwareService) gridDialect ).injectServices( serviceRegistry );
		}
	}

	/*
	 * @see org.hibernate.service.spi.Configurable
	 */

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
		sb.append( "]" );

		return sb.toString();
	}
}
