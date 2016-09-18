/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl;

import java.util.Map;

import org.hibernate.ogm.datastore.neo4j.dialect.impl.BaseNeo4jTypeConverter;
import org.hibernate.ogm.type.impl.ByteMappedType;
import org.hibernate.ogm.type.impl.IntegerMappedType;
import org.hibernate.ogm.type.impl.LongMappedType;
import org.hibernate.ogm.type.impl.PrimitiveByteArrayStringType;
import org.hibernate.ogm.type.impl.SerializableAsStringType;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * A type {@link BaseNeo4jTypeConverter} for remote Neo4j.
 *
 * @author Davide D'Alto
 */
public class BoltNeo4jTypeConverter extends BaseNeo4jTypeConverter {

	public static final BoltNeo4jTypeConverter INSTANCE = new BoltNeo4jTypeConverter();

	private static final Map<Type, GridType> conversionMap = createRemoteGridTypeConversionMap();

	private BoltNeo4jTypeConverter() {
	}

	private static Map<Type, GridType> createRemoteGridTypeConversionMap() {
		Map<Type, GridType> conversion = BaseNeo4jTypeConverter.createGridTypeConversionMap();
		conversion.put( StandardBasicTypes.INTEGER, IntegerMappedType.INSTANCE );
		conversion.put( StandardBasicTypes.LONG, LongMappedType.INSTANCE );
		conversion.put( StandardBasicTypes.BYTE, ByteMappedType.INSTANCE );
		conversion.put( StandardBasicTypes.BINARY, PrimitiveByteArrayStringType.INSTANCE );
		conversion.put( MaterializedBlobType.INSTANCE, PrimitiveByteArrayStringType.INSTANCE );
		return conversion;
	}

	@Override
	public GridType convert(Type type) {
		if ( type instanceof SerializableToBlobType ) {
			SerializableToBlobType<?> exposedType = (SerializableToBlobType<?>) type;
			return new SerializableAsStringType<>( exposedType.getJavaTypeDescriptor() );
		}
		return conversionMap.get( type );
	}
}
