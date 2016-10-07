/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl;

import java.util.Map;

import org.hibernate.ogm.datastore.neo4j.dialect.impl.BaseNeo4jTypeConverter;
import org.hibernate.ogm.type.impl.ByteMappedType;
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
public class HttpNeo4jTypeConverter extends BaseNeo4jTypeConverter {

	public static final HttpNeo4jTypeConverter INSTANCE = new HttpNeo4jTypeConverter();

	private static final Map<Type, GridType> conversionMap = createRemoteGridTypeConversionMap();

	private HttpNeo4jTypeConverter() {
	}

	private static Map<Type, GridType> createRemoteGridTypeConversionMap() {
		Map<Type, GridType> conversion = BaseNeo4jTypeConverter.createGridTypeConversionMap();
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
