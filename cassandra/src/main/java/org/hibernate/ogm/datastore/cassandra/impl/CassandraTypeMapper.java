/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.impl;

import org.hibernate.ogm.datastore.cassandra.type.impl.CassandraByteType;
import org.hibernate.ogm.datastore.cassandra.type.impl.CassandraCalendarDateType;
import org.hibernate.ogm.datastore.cassandra.type.impl.CassandraCalendarType;
import org.hibernate.ogm.datastore.cassandra.type.impl.CassandraCharacterType;
import org.hibernate.ogm.datastore.cassandra.type.impl.CassandraPrimitiveByteArrayType;
import org.hibernate.ogm.datastore.cassandra.type.impl.CassandraShortType;

import org.hibernate.ogm.type.impl.BooleanType;
import org.hibernate.ogm.type.impl.ClassType;
import org.hibernate.ogm.type.impl.DateType;
import org.hibernate.ogm.type.impl.DoubleType;
import org.hibernate.ogm.type.impl.EnumType;
import org.hibernate.ogm.type.impl.FloatType;
import org.hibernate.ogm.type.impl.IntegerType;
import org.hibernate.ogm.type.impl.LongType;
import org.hibernate.ogm.type.impl.StringType;
import org.hibernate.ogm.type.impl.TimeType;
import org.hibernate.ogm.type.impl.TimestampType;
import org.hibernate.ogm.type.impl.UrlType;

import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Assist the SchemaDefiner by supplying mapping from GridType to CQL3 column type.
 * see also http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/cql_data_types_c.html
 * see also CassandraDialect.overrideType()
 *
 * @author Jonathan Halliday
 */
public enum CassandraTypeMapper {

	INSTANCE;

	private Map<GridType, String> mapper = new HashMap<GridType, String>();

	{
		mapper.put( ClassType.INSTANCE, "blob" );
		mapper.put( LongType.INSTANCE, "bigint" );
		mapper.put( IntegerType.INSTANCE, "int" );
		mapper.put( DoubleType.INSTANCE, "double" );
		mapper.put( FloatType.INSTANCE, "float" );
		mapper.put( StringType.INSTANCE, "text" );
		mapper.put( UrlType.INSTANCE, "text" );
		mapper.put( BooleanType.INSTANCE, "boolean" );
		mapper.put( DateType.INSTANCE, "timestamp" );
		mapper.put( TimestampType.INSTANCE, "timestamp" );
		mapper.put( TimeType.INSTANCE, "timestamp" );

		mapper.put( CassandraCharacterType.INSTANCE, "text" );
		mapper.put( CassandraByteType.INSTANCE, "int" );
		mapper.put( CassandraShortType.INSTANCE, "int" );
		mapper.put( CassandraCalendarDateType.INSTANCE, "timestamp" );
		mapper.put( CassandraCalendarType.INSTANCE, "timestamp" );
		mapper.put( CassandraPrimitiveByteArrayType.INSTANCE, "blob" );
	}

	public String hibernateToCQL(GridType gridType) {

		String cqlType = mapper.get( gridType );

		if ( gridType instanceof EnumType ) {
			EnumType enumType = (EnumType) gridType;
			if ( enumType.isOrdinal() ) {
				return "int";
			}
			else {
				return "text";
			}
		}

		// attempt a sane default for anything we don't recognise
		if ( cqlType == null ) {
			cqlType = "text";
		}

		return cqlType;
	}

	public GridType overrideType(Type type) {

		if ( type == StandardBasicTypes.CALENDAR ) {
			return CassandraCalendarType.INSTANCE;
		}

		if ( type == StandardBasicTypes.CALENDAR_DATE ) {
			return CassandraCalendarDateType.INSTANCE;
		}

		if ( type == StandardBasicTypes.MATERIALIZED_BLOB || type == StandardBasicTypes.BINARY ) {
			return CassandraPrimitiveByteArrayType.INSTANCE;
		}

		if ( type == StandardBasicTypes.BYTE ) {
			return CassandraByteType.INSTANCE;
		}

		if ( type == StandardBasicTypes.SHORT ) {
			return CassandraShortType.INSTANCE;
		}

		if ( type == StandardBasicTypes.CHARACTER ) {
			return CassandraCharacterType.INSTANCE;
		}

		return null;
	}
}
