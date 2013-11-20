/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.couchdb;

import java.util.Iterator;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastore;
import org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.couchdb.json.CouchDBAssociation;
import org.hibernate.ogm.dialect.couchdb.json.CouchDBEntity;
import org.hibernate.ogm.dialect.couchdb.model.CouchDBAssociationSnapshot;
import org.hibernate.ogm.dialect.couchdb.model.CouchDBTupleSnapshot;
import org.hibernate.ogm.dialect.couchdb.type.CouchDBBlobType;
import org.hibernate.ogm.dialect.couchdb.type.CouchDBByteType;
import org.hibernate.ogm.dialect.couchdb.type.CouchDBDateType;
import org.hibernate.ogm.dialect.couchdb.type.CouchDBLongType;
import org.hibernate.ogm.dialect.couchdb.type.CouchDBTimeType;
import org.hibernate.ogm.dialect.couchdb.util.Identifier;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.StringCalendarDateType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Stores Tuples and Associations as Documents inside CouchDB
 *
 * Tuples are stored in CouchDB Documents obtained as a JSON serialization of a {@link CouchDBEntity} object
 *
 * Associations are stored in CouchDB Documents obtained as a JSON serialization of a {@link CouchDBAssociation} object
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class CouchDBDialect implements GridDialect {

	private final CouchDBDatastoreProvider provider;

	public CouchDBDialect(CouchDBDatastoreProvider provider) {
		this.provider = provider;
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		return null;
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		CouchDBEntity entity = getDataStore().getEntity( Identifier.createEntityId( key ) );
		if ( entity != null ) {
			return new Tuple( new CouchDBTupleSnapshot( entity.getProperties() ) );
		}

		return null;
	}

	@Override
	public Tuple createTuple(EntityKey key) {
		return new Tuple( new CouchDBTupleSnapshot( key ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key) {
		CouchDBEntity entity = getDataStore().getEntity( Identifier.createEntityId( key ) );
		if ( entity == null ) {
			entity = new CouchDBEntity( key );
		}
		entity.update( tuple );
		getDataStore().saveDocument( entity );
	}

	@Override
	public void removeTuple(EntityKey key) {
		removeDocumentIfPresent( Identifier.createEntityId( key ) );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		CouchDBAssociation association = getDataStore().getAssociation( Identifier.createAssociationId( key ) );
		if ( association != null ) {
			return new Association( new CouchDBAssociationSnapshot( association, key ) );
		}
		return null;
	}

	@Override
	public Association createAssociation(AssociationKey key) {
		CouchDBAssociation association = new CouchDBAssociation( Identifier.createAssociationId( key ) );
		return new Association( new CouchDBAssociationSnapshot( association, key ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key) {
		CouchDBAssociation couchDBAssociation = getDataStore().getAssociation( Identifier.createAssociationId( key ) );
		if ( couchDBAssociation == null ) {
			couchDBAssociation = new CouchDBAssociation( Identifier.createAssociationId( key ) );
		}
		couchDBAssociation.update( association, key );
		getDataStore().saveDocument( couchDBAssociation );
	}

	@Override
	public void removeAssociation(AssociationKey key) {
		removeDocumentIfPresent( Identifier.createAssociationId( key ) );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return new Tuple( new CouchDBTupleSnapshot() );
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		value.initialize( getDataStore().nextValue( key, increment, initialValue ) );
	}

	@Override
	public GridType overrideType(Type type) {
		if ( type == StandardBasicTypes.MATERIALIZED_BLOB ) {
			return CouchDBBlobType.INSTANCE;
		}
		else if ( type == StandardBasicTypes.CALENDAR || type == StandardBasicTypes.CALENDAR_DATE ) {
			return StringCalendarDateType.INSTANCE;
		}
		else if ( type == StandardBasicTypes.DATE ) {
			return CouchDBDateType.INSTANCE;
		}
		else if ( type == StandardBasicTypes.TIMESTAMP ) {
			return CouchDBDateType.INSTANCE;
		}
		else if ( type == StandardBasicTypes.BYTE ) {
			return CouchDBByteType.INSTANCE;
		}
		else if ( type == StandardBasicTypes.TIME ) {
			return CouchDBTimeType.INSTANCE;
		}
		else if ( type == StandardBasicTypes.LONG ) {
			return CouchDBLongType.INSTANCE;
		}
		return null;
	}

	@Override
	public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
			forTuple( consumer, entityKeyMetadata );
		}
	}

	/**
	 * Returns the number of Asosciations stored inside the database
	 *
	 * @return the number of Asosciations
	 */
	public int getAssociationSize() {
		return getDataStore().getNumberOfAssociations();
	}

	/**
	 * Returns the number of Entities stored inside the database
	 *
	 * @return the number of Entities
	 */
	public int getEntitiesSize() {
		return getDataStore().getNumberOfEntities();
	}

	private void forTuple(Consumer consumer, EntityKeyMetadata entityKeyMetadata) {
		List<Tuple> tuples = getTuples( entityKeyMetadata );
		for ( Tuple tuple : tuples ) {
			consumer.consume( tuple );
		}
	}

	private List<Tuple> getTuples(EntityKeyMetadata entityKeyMetadata) {
		return getDataStore().getTuples( entityKeyMetadata );
	}

	private CouchDBDatastore getDataStore() {
		return provider.getDataStore();
	}

	private void removeDocumentIfPresent(String id) {
		String currentRevision = getDataStore().getCurrentRevision( id );
		if ( currentRevision != null ) {
			getDataStore().deleteDocument( id, currentRevision );
		}
	}

	@Override
	public Iterator<Tuple> executeBackendQuery(CustomQuery customQuery, EntityKeyMetadata[] metadatas) {
		throw new UnsupportedOperationException( "Native queries not supported for CouchDB" );
	}

}
