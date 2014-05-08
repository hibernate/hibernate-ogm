/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.couchdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.impl.CouchDBDatastore;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.AssociationDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.model.impl.CouchDBAssociation;
import org.hibernate.ogm.datastore.couchdb.dialect.model.impl.CouchDBAssociationSnapshot;
import org.hibernate.ogm.datastore.couchdb.dialect.model.impl.CouchDBTupleSnapshot;
import org.hibernate.ogm.datastore.couchdb.dialect.type.impl.CouchDBBlobType;
import org.hibernate.ogm.datastore.couchdb.dialect.type.impl.CouchDBByteType;
import org.hibernate.ogm.datastore.couchdb.dialect.type.impl.CouchDBLongType;
import org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastoreProvider;
import org.hibernate.ogm.datastore.couchdb.logging.impl.Log;
import org.hibernate.ogm.datastore.couchdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.couchdb.util.impl.Identifier;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.impl.AssociationStorageOption;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.TupleIterator;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKind;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.query.NoOpParameterMetadataBuilder;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.Iso8601StringCalendarType;
import org.hibernate.ogm.type.Iso8601StringDateType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Stores tuples and associations as JSON documents inside CouchDB.
 * <p>
 * Tuples are stored in CouchDB documents obtained as a JSON serialization of a {@link EntityDocument} object.
 * Associations are stored in CouchDB documents obtained as a JSON serialization of a {@link AssociationDocument} object.
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 * @author Gunnar Morling
 */
public class CouchDBDialect implements GridDialect {

	private static final Log log = LoggerFactory.getLogger();

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
		EntityDocument entity = getDataStore().getEntity( Identifier.createEntityId( key ) );
		if ( entity != null ) {
			return new Tuple( new CouchDBTupleSnapshot( entity.getProperties() ) );
		}

		return null;
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		return new Tuple( new CouchDBTupleSnapshot( key ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		CouchDBTupleSnapshot snapshot = (CouchDBTupleSnapshot) tuple.getSnapshot();

		String revision = (String) snapshot.get( Document.REVISION_FIELD_NAME );

		// load the latest revision for updates without the revision being present; a warning about
		// this mapping will have been issued at factory start-up
		if ( revision == null && !snapshot.isCreatedOnInsert() ) {
			revision = getDataStore().getCurrentRevision( Identifier.createEntityId( key ), false );
		}

		// this will raise an optimistic locking exception if the revision is either null or not the current one
		getDataStore().saveDocument( new EntityDocument( key, revision, tuple ) );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		removeDocumentIfPresent( Identifier.createEntityId( key ) );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		CouchDBAssociation couchDBAssociation = null;

		if ( isStoredInEntityStructure( key, associationContext ) ) {
			EntityDocument owningEntity = getDataStore().getEntity( Identifier.createEntityId( key.getEntityKey() ) );
			if ( owningEntity != null && owningEntity.getProperties().containsKey( key.getCollectionRole() ) ) {
				couchDBAssociation = CouchDBAssociation.fromEmbeddedAssociation( owningEntity, key.getCollectionRole() );
			}
		}
		else {
			AssociationDocument association = getDataStore().getAssociation( Identifier.createAssociationId( key ) );
			if ( association != null ) {
				couchDBAssociation = CouchDBAssociation.fromAssociationDocument( association );
			}
		}

		return couchDBAssociation != null ? new Association( new CouchDBAssociationSnapshot( couchDBAssociation, key ) ) : null;
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		CouchDBAssociation couchDBAssociation = null;

		if ( isStoredInEntityStructure( key, associationContext ) ) {
			EntityDocument owningEntity = getDataStore().getEntity( Identifier.createEntityId( key.getEntityKey() ) );
			if ( owningEntity == null ) {
				owningEntity = (EntityDocument) getDataStore().saveDocument( new EntityDocument( key.getEntityKey() ) );
			}

			couchDBAssociation = CouchDBAssociation.fromEmbeddedAssociation( owningEntity, key.getCollectionRole() );
		}
		else {
			AssociationDocument association = new AssociationDocument( Identifier.createAssociationId( key ) );
			couchDBAssociation = CouchDBAssociation.fromAssociationDocument( association );
		}

		return new Association( new CouchDBAssociationSnapshot( couchDBAssociation, key ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey associationKey, AssociationContext associationContext) {
		List<Map<String, Object>> rows = getAssociationRows( association, associationKey );

		CouchDBAssociation couchDBAssociation = ( (CouchDBAssociationSnapshot) association.getSnapshot() ).getCouchDbAssociation();
		couchDBAssociation.setRows( rows );

		getDataStore().saveDocument( couchDBAssociation.getOwningDocument() );
	}

	private List<Map<String, Object>> getAssociationRows(Association association, AssociationKey associationKey) {
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>( association.getKeys().size() );

		for ( RowKey rowKey : association.getKeys() ) {
			Tuple tuple = association.get( rowKey );

			Map<String, Object> row = new HashMap<String, Object>( 3 );
			for ( String columnName : tuple.getColumnNames() ) {
				// don't store columns which are part of the association key and can be retrieved from there
				if ( !associationKey.isKeyColumn( columnName ) ) {
					row.put( columnName, tuple.get( columnName ) );
				}
			}

			rows.add( row );
		}
		return rows;
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		if ( isStoredInEntityStructure( key, associationContext ) ) {
			EntityDocument owningEntity = getDataStore().getEntity( Identifier.createEntityId( key.getEntityKey() ) );
			if ( owningEntity != null ) {
				owningEntity.removeAssociation( key.getCollectionRole() );
				getDataStore().saveDocument( owningEntity );
			}
		}
		else {
			removeDocumentIfPresent( Identifier.createAssociationId( key ) );
		}
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return new Tuple();
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
		AssociationStorageType associationStorage = associationContext
				.getOptionsContext()
				.getUnique( AssociationStorageOption.class );

		return associationKey.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION ||
				associationStorage == AssociationStorageType.IN_ENTITY;
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
		// persist calendars as ISO8601 strings, including TZ info
		else if ( type == StandardBasicTypes.CALENDAR ) {
			return Iso8601StringCalendarType.DATE_TIME;
		}
		else if ( type == StandardBasicTypes.CALENDAR_DATE ) {
			return Iso8601StringCalendarType.DATE;
		}
		// persist date as ISO8601 strings, in UTC, without TZ info
		else if ( type == StandardBasicTypes.DATE ) {
			return Iso8601StringDateType.DATE;
		}
		else if ( type == StandardBasicTypes.TIME ) {
			return Iso8601StringDateType.TIME;
		}
		else if ( type == StandardBasicTypes.TIMESTAMP ) {
			return Iso8601StringDateType.DATE_TIME;
		}
		else if ( type == StandardBasicTypes.BYTE ) {
			return CouchDBByteType.INSTANCE;
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
		String currentRevision = getDataStore().getCurrentRevision( id, false );
		if ( currentRevision != null ) {
			getDataStore().deleteDocument( id, currentRevision );
		}
	}

	@Override
	public TupleIterator executeBackendQuery(BackendCustomQuery customQuery, QueryParameters queryParameters, EntityKeyMetadata[] metadatas) {
		throw new UnsupportedOperationException( "Native queries not supported for CouchDB" );
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return new NoOpParameterMetadataBuilder();
	}
}
