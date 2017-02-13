/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.constant;

import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.BigIntegerType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.BooleanType;
import org.hibernate.type.ByteType;
import org.hibernate.type.CalendarDateType;
import org.hibernate.type.CalendarType;
import org.hibernate.type.CharacterType;
import org.hibernate.type.DateType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.FloatType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.MaterializedClobType;
import org.hibernate.type.NumericBooleanType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.ShortType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimeType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.TrueFalseType;
import org.hibernate.type.UUIDBinaryType;
import org.hibernate.type.UrlType;
import org.hibernate.type.YesNoType;

/**
 * Collection of mappings
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDBMapping {

	/**
	 * Mapping from Hibernate data type to OrientDB data type
	 */
	@SuppressWarnings("rawtypes")
	public static final Map<Class, String> TYPE_MAPPING = getTypeMapping();
	/**
	 * Mapping from SQL data type to OrientDB data type
	 */
	public static final Map<Integer, String> SQL_TYPE_MAPPING = getSqlTypeMapping();

	/**
	 * Mapping of types for generate sequence
	 */
	@SuppressWarnings("rawtypes")
	public static final Set<Class> SEQ_TYPES = getSeqTypes();
	/**
	 * Mapping of types for identity relations (One-To-One and One-To-Many)
	 */
	@SuppressWarnings("rawtypes")
	public static final Set<Class> RELATIONS_TYPES = getRelationsTypes();

	/**
	 * Mapping of types for generate fields for foreign linking
	 */
	@SuppressWarnings("rawtypes")
	public static final Map<Class, Class> FOREIGN_KEY_TYPE_MAPPING = getForeignKeyTypeMapping();

	private static Map<Integer, String> getSqlTypeMapping() {
		Map<Integer, String> map = new HashMap<>();
		map.put( Types.VARCHAR, "string" );
		map.put( Types.CHAR, "string" );

		map.put( Types.FLOAT, "float" );
		map.put( Types.DOUBLE, "double" );
		map.put( Types.INTEGER, "integer" );
		map.put( Types.SMALLINT, "short" );
		map.put( Types.DECIMAL, "decimal" );

		map.put( Types.BINARY, "binary" );
		map.put( Types.LONGVARBINARY, "binary" );

		map.put( Types.BOOLEAN, "boolean" );
		map.put( Types.DATE, "date" );

		return Collections.unmodifiableMap( map );
	}

	@SuppressWarnings("rawtypes")
	private static Map<Class, String> getTypeMapping() {
		Map<Class, String> map = new HashMap<>();

		map.put( ByteType.class, "byte" );
		map.put( IntegerType.class, "integer" );
		map.put( NumericBooleanType.class, "short" );
		map.put( ShortType.class, "short" );
		map.put( LongType.class, "long" );
		map.put( FloatType.class, "float" );
		map.put( DoubleType.class, "double" );
		map.put( DateType.class, "date" );
		map.put( CalendarDateType.class, "date" );
		map.put( TimestampType.class, "datetime" );
		map.put( CalendarType.class, "datetime" );
		map.put( TimeType.class, "datetime" );

		map.put( BooleanType.class, "boolean" );

		map.put( TrueFalseType.class, "string" );
		map.put( YesNoType.class, "string" );
		map.put( StringType.class, "string" );
		map.put( UrlType.class, "string" );

		map.put( CharacterType.class, "string" );
		map.put( UUIDBinaryType.class, "string" );

		map.put( BigIntegerType.class, "string" );

		map.put( BinaryType.class, "binary" ); // byte[]
		map.put( MaterializedBlobType.class, "binary" ); // byte[]
		map.put( SerializableToBlobType.class, "binary" ); // byte[]
		map.put( MaterializedClobType.class, "binary" );

		map.put( BigDecimalType.class, "decimal" );
		return Collections.unmodifiableMap( map );

	}

	@SuppressWarnings("rawtypes")
	private static Set<Class> getSeqTypes() {
		Set<Class> set1 = new HashSet<>();
		set1.add( IntegerType.class );
		set1.add( LongType.class );
		return Collections.unmodifiableSet( set1 );
	}

	@SuppressWarnings("rawtypes")
	private static Set<Class> getRelationsTypes() {
		Set<Class> set2 = new HashSet<>();
		set2.add( ManyToOneType.class );
		set2.add( OneToOneType.class );
		return Collections.unmodifiableSet( set2 );
	}

	@SuppressWarnings("rawtypes")
	private static Map<Class, Class> getForeignKeyTypeMapping() {
		Map<Class, Class> map1 = new HashMap<>();
		map1.put( Long.class, LongType.class );
		map1.put( Integer.class, IntegerType.class );
		map1.put( String.class, StringType.class );
		return Collections.unmodifiableMap( map1 );
	}

}
