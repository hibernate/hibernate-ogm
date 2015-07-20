/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchbase;

import static org.hibernate.ogm.datastore.document.util.impl.Identifier.createEntityId;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.couchbase.dialect.model.impl.CouchBaseDatastore;
import org.hibernate.ogm.datastore.couchbase.dialect.type.impl.CouchBaseBlobType;
import org.hibernate.ogm.datastore.couchbase.dialect.type.impl.CouchBaseByteType;
import org.hibernate.ogm.datastore.couchbase.impl.CouchBaseDatastoreProvider;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.impl.Iso8601StringCalendarType;
import org.hibernate.ogm.type.impl.Iso8601StringDateType;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;

/**
 *
 * @author Stawicka Ewa
 */
public class CouchBaseDialect extends BaseGridDialect {

	private final CouchBaseDatastoreProvider provider;

	public CouchBaseDialect(CouchBaseDatastoreProvider provider) {
		this.provider = provider;
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		JsonDocument document = getDataStore().getEntity( createEntityId( key ) );
		return document == null ? null : new Tuple( new MapTupleSnapshot( document.content().toMap() ) );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		Map<String, Object> id = new HashMap<String, Object>();
		for (int i = 0; i < key.getColumnNames().length; ++i) {
			id.put( key.getColumnNames()[i], key.getColumnValues()[i] );
		}
		return new Tuple( new MapTupleSnapshot( id ) );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext)
					throws TupleAlreadyExistsException {
		JsonObject content = JsonObject.empty();
		if ( tuple != null ) {
			for ( String columnName : tuple.getColumnNames() ) {

				Object value = tuple.get( columnName );
				if ( value != null ) {
					content.put( columnName, value );
				}
			}
		}

		String id = createEntityId( key );
		JsonDocument document = JsonDocument.create( id, content );

		getDataStore().saveDocument( document );

	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		getDataStore().deleteDocument( createEntityId( key ) );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association,
					AssociationContext associationContext) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return getDataStore().nextValue( request.getKey(), request.getIncrement(), request.getInitialValue() );
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
	}

	private CouchBaseDatastore getDataStore() {
		return provider.getDataStore();
	}

	@Override
	public GridType overrideType(Type type) {
		if ( type == StandardBasicTypes.MATERIALIZED_BLOB ) {
			return CouchBaseBlobType.INSTANCE;
		}
		// persist calendars as ISO8601 strings, including TZ info
		else if (type == StandardBasicTypes.CALENDAR) {
			return Iso8601StringCalendarType.DATE_TIME;
		}
		else if (type == StandardBasicTypes.CALENDAR_DATE) {
			return Iso8601StringCalendarType.DATE;
		}
		// persist date as ISO8601 strings, in UTC, without TZ info
		else if (type == StandardBasicTypes.DATE) {
			return Iso8601StringDateType.DATE;
		}
		else if (type == StandardBasicTypes.TIME) {
			return Iso8601StringDateType.TIME;
		}
		else if (type == StandardBasicTypes.TIMESTAMP) {
			return Iso8601StringDateType.DATE_TIME;
		}
		else if (type == StandardBasicTypes.BYTE) {
			return CouchBaseByteType.INSTANCE;
		}
		else if (type == BinaryType.INSTANCE) {
			return CouchBaseBlobType.INSTANCE;
		}

		return null;
	}

}
