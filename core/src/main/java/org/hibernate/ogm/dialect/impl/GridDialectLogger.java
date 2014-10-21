/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.io.Serializable;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.CoreLogCategories;
import org.hibernate.ogm.util.impl.Log;
import org.jboss.logging.Logger;

/**
 * A wrapper dialect that logs any performance-relevant calls performed on the real dialect.
 * It is only used when this class's logger level is set to Trace
 *
 * @author Sebastien Lorber (<i>lorber.sebastien@gmail.com</i>)
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 *
 * @see GridDialectInitiator
 */
public class GridDialectLogger extends ForwardingGridDialect<Serializable> {

	private static final Log log = Logger.getMessageLogger( Log.class, CoreLogCategories.DATASTORE_ACCESS.toString() );

	public GridDialectLogger(GridDialect gridDialect) {
		super( gridDialect );
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
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		log.tracef( "Reading Tuple with key %1$s and context %2$s", key, tupleContext );
		return super.getTuple( key, tupleContext );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		log.tracef( "Build Tuple object with key %1$s (does not trigger access to the datastore)", key );
		return super.createTuple( key, tupleContext );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		if ( tuple.getSnapshot().isEmpty() ) {
			log.tracef( "Creating Tuple with key %1$s in datastore", key );
		}
		else {
			log.tracef( "Updating Tuple with key %1$s in datastore", key );
		}
		super.insertOrUpdateTuple( key, tuple, tupleContext );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		log.tracef( "Removing Tuple with key %1$s from datastore", key );
		super.removeTuple( key, tupleContext );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		log.tracef( "Reading association with key %1$s from datastore and context %2$s", key, associationContext );
		return super.getAssociation( key, associationContext );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		log.tracef( "Build association object with key %1$s (does not trigger access to the datastore)", key );
		return super.createAssociation( key, associationContext );
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		if ( association.getSnapshot().size() == 0 ) {
			log.tracef( "Creating association with key %1$s in datastore", key );
		}
		else {
			log.tracef( "Updating association with key %1$s in datastore", key );
		}
		super.insertOrUpdateAssociation( key, association, associationContext );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		log.tracef( "Removing association with key %1$s from datastore", key );
		super.removeAssociation( key, associationContext );
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		log.tracef( "Extracting next value from key %1$s", request.getKey() );
		return super.nextValue( request );
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		log.tracef( "Determining whether association %1$s is stored in an entity structure", associationKeyMetadata );
		return super.isStoredInEntityStructure( associationKeyMetadata, associationTypeContext );
	}

	@Override
	public void executeBatch(OperationsQueue queue) {
		log.tracef( "Executing batch with %1$s items", queue.size() );
		super.executeBatch( queue );
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<Serializable> query, QueryParameters queryParameters) {
		log.tracef( "Executing backend query: %1$s", query.getQuery() );
		return super.executeBackendQuery( query, queryParameters );
	}
}
