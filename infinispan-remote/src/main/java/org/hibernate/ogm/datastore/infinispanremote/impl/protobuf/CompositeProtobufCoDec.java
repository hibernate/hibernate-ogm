/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.hibernate.ogm.datastore.infinispanremote.impl.VersionedAssociation;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.MainOgmCoDec;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamAssociationPayload;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamId;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamPayload;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

public final class CompositeProtobufCoDec implements MainOgmCoDec {

	private final String tableName;
	private final String protobufTypeName;
	private final String protobufIdTypeName;
	private final RemoteCache remoteCache;
	private final ProtofieldWriterSet keyFields;
	private final ProtofieldWriterSet valueFields;
	private final SchemaDefinitions sd;

	public CompositeProtobufCoDec(String tableName, String protobufTypeName, String protobufIdTypeName, ProtofieldWriterSet keyFields, ProtofieldWriterSet valueFields, RemoteCache remoteCache, SchemaDefinitions sd) {
		this.tableName = tableName;
		this.protobufTypeName = protobufTypeName;
		this.protobufIdTypeName = protobufIdTypeName;
		this.remoteCache = remoteCache;
		this.keyFields = keyFields;
		this.valueFields = valueFields;
		this.sd = sd;
	}

	@Override
	public RemoteCache getLinkedCache() {
		return remoteCache;
	}

	@Override
	public ProtostreamId readProtostreamId(ProtoStreamReader reader) throws IOException {
		final int size = keyFields.size();
		final String[] columnNames = new String[size];
		final Object[] columnValues = new Object[size];
		for ( int i = 0; i < size; i++ ) {
			final UnsafeProtofield protoField = keyFields.getDecoderByListOrder( i );
			columnNames[i] = protoField.getColumnName();
			columnValues[i] = protoField.read( reader );
		}
		return createIdPayload( columnNames, columnValues );
	}

	@Override
	public ProtostreamId createIdPayload(String[] columnNames, Object[] columnValues) {
		assert verifyAllColumnNamesArePartOfId( columnNames );
		return new ProtostreamId( columnNames, columnValues );
	}

	private boolean verifyAllColumnNamesArePartOfId(String[] columnNames) {
		for ( String name : columnNames ) {
			if ( ! keyFields.columnNameExists( name ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void writeIdTo(ProtoStreamWriter writer, ProtostreamId id) throws IOException {
		final int size = id.columnNames.length;
		for ( int i = 0; i < size; i++ ) {
			final String columnName = id.columnNames[i];
			UnsafeProtofield protofieldWriter = keyFields.getDecoderByColumnName( columnName );
			if ( protofieldWriter == null ) {
				//Sometimes the Association Key contains columns beyond the strictly required ones for the key
				continue;
			}
			protofieldWriter.writeTo( writer, id.columnValues[i] );
		}
	}

	@Override
	public ProtostreamPayload readPayloadFrom(ProtoStreamReader reader) throws IOException {
		final int size = valueFields.size();
		Map mapTuple = new HashMap<>( size );
		for ( int i = 0; i < size; i++ ) {
			final UnsafeProtofield protoField = valueFields.getDecoderByListOrder( i );
			final String column = protoField.getColumnName();
			final Object value = protoField.read( reader );
			if ( value != null ) {
				mapTuple.put( column, value );
			}
		}
		MapTupleSnapshot loadedSnapshot = new MapTupleSnapshot( mapTuple );
		return new ProtostreamPayload( loadedSnapshot );
	}

	@Override
	public void writePayloadTo(ProtoStreamWriter writer, ProtostreamPayload payload) {
		//N.B. we iterate in order by tag number of the protobuf field, as this affects encoding performance
		//This implies we might be requesting columns which are not defined in the payload
		valueFields.forEachProtostreamMappedField( f -> {
			Object columnValue = payload.getColumnValue( f.getColumnName() );
			if ( columnValue != null ) {
				f.writeTo( writer, columnValue );
			}
		} );
	}

	@Override
	public ProtostreamPayload createValuePayload(Tuple tuple) {
		return new ProtostreamPayload( tuple );
	}

	@Override
	public String getProtobufTypeName() {
		return protobufTypeName;
	}

	@Override
	public String getIdProtobufTypeName() {
		return protobufIdTypeName;
	}

	@Override
	public ProtostreamAssociationPayload createAssociationPayload(EntityKey key, VersionedAssociation assoc, TupleContext tupleContext) {
		return new ProtostreamAssociationPayload( assoc );
	}

	@Override
	public String convertColumnNameToFieldName(String columnName) {
		return valueFields.getDecoderByColumnName( columnName ).getProtobufName();
	}

	/**
	 * Checks that both conditions hold true:
	 * the listed columns are sufficient to create and ID (a primary key)
	 * and exceed the requirement: there's at least one additional column
	 * to narrow down the selection.
	 * ASSUMPTION: not expecting duplicates among the columnNames.
	 */
	@Override
	public boolean columnSetExceedsIdRequirements(String[] columnNames) {
		assert namesAreUnique( columnNames );
		int keySetSize = keyFields.size();
		//First check that we have a larger set:
		if ( columnNames.length <= keySetSize ) {
			return false;
		}
		//Now check that we cover at least all same names of the required PK column names:
		int columnsOfPK = 0;
		for ( String columnName : columnNames ) {
			if ( keyFields.columnNameExists( columnName ) ) {
				columnsOfPK++;
			}
		}
		return columnsOfPK == keySetSize;
	}

	private boolean namesAreUnique(String[] columnNames) {
		HashSet<String> existing = new HashSet<>();
		for ( String name : columnNames ) {
			existing.add( name );
		}
		return columnNames.length == existing.size();
	}

	@Override
	public String[] listIdColumnNames() {
		return keyFields.getColumnNames();
	}

}
