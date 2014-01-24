/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.dialect.impl;

import java.util.Iterator;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.event.internal.AbstractFlushingEventListener;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.BatchableGridDialect;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.batch.OperationsQueue;
import org.hibernate.ogm.dialect.batch.RemoveAssociationOperation;
import org.hibernate.ogm.dialect.batch.RemoveTupleOperation;
import org.hibernate.ogm.dialect.batch.UpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.UpdateTupleOperation;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.service.impl.OgmEventListener;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;

/**
 * Batch the dialect operations and delegate the execute to a {@link BatchableGridDialect} when a flush or auto_flush
 * event is triggered or when required.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class BatchableDelegator implements GridDialect, ServiceRegistryAwareService {

	private final BatchableGridDialect dialect;

	private final ThreadLocal<OperationsQueue> operationsQueue = new ThreadLocal<OperationsQueue>();

	public BatchableDelegator(BatchableGridDialect dialect) {
		this.dialect = dialect;
	}

	public void prepareBatch() {
		operationsQueue.set( new OperationsQueue() );
	}

	public void executeBatch() {
		if ( operationsQueue.get() != null ) {
			dialect.executeBatch( operationsQueue.get() );
			operationsQueue.set( null );
		}
	}

	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		return dialect.getLockingStrategy( lockable, lockMode );
	}

	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		if ( operationsQueue.get() != null && operationsQueue.get().contains( key ) ) {
			flush();
		}
		return dialect.getTuple( key, tupleContext );
	}

	public Tuple createTuple(EntityKey key) {
		return dialect.createTuple( key );
	}

	public void updateTuple(final Tuple tuple, final EntityKey key) {
		if ( operationsQueue.get() == null ) {
			dialect.updateTuple( tuple, key );
		}
		else {
			operationsQueue.get().add( new UpdateTupleOperation( tuple, key ) );
		}
	}

	public void removeTuple(final EntityKey key) {
		if ( operationsQueue.get() == null ) {
			dialect.removeTuple( key );
		}
		else {
			operationsQueue.get().add( new RemoveTupleOperation( key ) );
		}
	}

	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		return dialect.getAssociation( key, associationContext );
	}

	public Association createAssociation(AssociationKey key, final AssociationContext context) {
		return dialect.createAssociation( key, context );
	}

	public void updateAssociation(final Association association, final AssociationKey key, final AssociationContext context) {
		if ( operationsQueue.get() == null ) {
			dialect.updateAssociation( association, key, context );
		}
		else {
			operationsQueue.get().add( new UpdateAssociationOperation( association, key, context ) );
		}
	}

	public void removeAssociation(final AssociationKey key, final AssociationContext context) {
		if ( operationsQueue.get() == null ) {
			dialect.removeAssociation( key, context );
		}
		else {
			operationsQueue.get().add( new RemoveAssociationOperation( key, context ) );
		}
	}

	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		flush();
		return dialect.createTupleAssociation( associationKey, rowKey );
	}

	private void flush() {
		if ( operationsQueue.get() != null ) {
			dialect.executeBatch( operationsQueue.get() );
		}
	}

	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		dialect.nextValue( key, value, increment, initialValue );
	}

	public GridType overrideType(Type type) {
		return dialect.overrideType( type );
	}

	public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		dialect.forEachTuple( consumer, entityKeyMetadatas );
	}

	public Iterator<Tuple> executeBackendQuery(CustomQuery customQuery, EntityKeyMetadata[] metadatas) {
		return dialect.executeBackendQuery( customQuery, metadatas );
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		listenerRegistry.addDuplicationStrategy( new FlushListenerDuplicationStrategy() );
		listenerRegistry.getEventListenerGroup( EventType.FLUSH ).appendListener( new OgmEventListener( this ) );
		//need a different wrapper instance as we delegate the work to different delegates
		listenerRegistry.getEventListenerGroup( EventType.AUTO_FLUSH ).appendListener( new OgmEventListener( this ) );
	}

	public static class FlushListenerDuplicationStrategy implements DuplicationStrategy {

		@Override
		public boolean areMatch(Object listener, Object original) {
			boolean match = original instanceof AbstractFlushingEventListener && listener instanceof OgmEventListener;
			if ( match ) {
				( (OgmEventListener) listener ).setDelegate( original );
			}
			return match;
		}

		@Override
		public Action getAction() {
			return Action.REPLACE_ORIGINAL;
		}

	}

}
