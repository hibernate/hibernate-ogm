/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect;

import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.impl.GridDialectInitiator;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.IdGeneratorKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.util.ClosableIterator;
import org.hibernate.ogm.util.impl.CoreLogCategories;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

/**
 * A wrapper dialect that logs any performance-relevant calls performed on the real dialect.
 * It is only used when this class's logger level is set to Trace
 *
 * @author Sebastien Lorber (<i>lorber.sebastien@gmail.com</i>)
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 *
 * @see GridDialectInitiator
 */
public class GridDialectLogger implements GridDialect, Configurable, ServiceRegistryAwareService {

	private static final Log log = Logger.getMessageLogger( Log.class, CoreLogCategories.DATASTORE_ACCESS.toString() );

	private final GridDialect gridDialect; // the real wrapped grid dialect

	public GridDialectLogger(GridDialect gridDialect) {
		if ( gridDialect == null ) {
			throw new IllegalArgumentException( "GridDialect should never be null" );
		}
		this.gridDialect = gridDialect;
	}

	/**
	 * Returns true if this grid dialect logger should wrap the real grid dialect
	 *
	 * @return boolean
	 */
	public static boolean activationNeeded() {
		return log.isTraceEnabled();
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		return gridDialect.getLockingStrategy( lockable, lockMode );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		log.tracef( "Reading Tuple with key %1$s and context %2$s", key, tupleContext.toString() );
		return gridDialect.getTuple( key, tupleContext );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		log.tracef( "Build Tuple object with key %1$s (does not trigger access to the datastore)", key );
		return gridDialect.createTuple( key, tupleContext );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		if ( tuple.getSnapshot().isEmpty() ) {
			log.tracef( "Creating Tuple with key %1$s in datastore", key );
		}
		else {
			log.tracef( "Updating Tuple with key %1$s in datastore", key );
		}
		gridDialect.updateTuple( tuple, key, tupleContext );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		log.tracef( "Removing Tuple with key %1$s from datastore", key );
		gridDialect.removeTuple( key, tupleContext );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		log.tracef( "Reading association with key %1$s from datastore and context %2$s", key, associationContext );
		return gridDialect.getAssociation( key, associationContext );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		log.tracef( "Build association object with key %1$s (does not trigger access to the datastore)", key );
		return gridDialect.createAssociation( key, associationContext );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext) {
		if ( association.getSnapshot().size() == 0 ) {
			log.tracef( "Creating association with key %1$s in datastore", key );
		}
		else {
			log.tracef( "Updating association with key %1$s in datastore", key );
		}
		gridDialect.updateAssociation( association, key, associationContext );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		log.tracef( "Removing association with key %1$s from datastore", key );
		gridDialect.removeAssociation( key, associationContext );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		log.tracef(
				"Build Tuple object for row key entry %1$s in association %2$s (does not trigger access to the datastore)",
				rowKey,
				associationKey
		);
		return gridDialect.createTupleAssociation( associationKey, rowKey );
	}

	@Override
	public void nextValue(IdGeneratorKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		log.tracef( "Extracting next value from key %1$s", key );
		gridDialect.nextValue( key, value, increment, initialValue );
	}

	@Override
	public GridType overrideType(Type type) {
		return gridDialect.overrideType( type );
	}

	@Override
	public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		gridDialect.forEachTuple( consumer, entityKeyMetadatas );
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendCustomQuery customQuery, QueryParameters queryParameters) {
		log.tracef( "Executing native backend query: %1$s", customQuery.getQueryString() );
		return gridDialect.executeBackendQuery( customQuery, queryParameters );
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return gridDialect.getParameterMetadataBuilder();
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
		log.tracef( "Determining whether assocication %1$s is stored in an entity structure", associationKey );
		return gridDialect.isStoredInEntityStructure( associationKey, associationContext );
	}

	@Override
	public void configure(Map configurationValues) {
		if ( gridDialect instanceof Configurable ) {
			log.tracef( "Configuring service with properties: %1$s", configurationValues );
			( (Configurable) gridDialect ).configure( configurationValues );
		}
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		if ( gridDialect instanceof ServiceRegistryAwareService ) {
			log.tracef( "Injecting service registry" );
			( (ServiceRegistryAwareService) gridDialect ).injectServices( serviceRegistry );
		}
	}
}
