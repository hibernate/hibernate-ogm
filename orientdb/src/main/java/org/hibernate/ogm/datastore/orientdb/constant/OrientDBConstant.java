/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.orientdb.constant;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Constants of the implementation
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */

@SuppressWarnings("rawtypes")
public class OrientDBConstant {

	/**
	 * Set of system fields of OrientDB document
	 */
	public static final Set<String> SYSTEM_FIELDS;
	/**
	 * Name of system property 'record id'
	 */
	public static final String SYSTEM_RID = "@rid";
	/**
	 * Name of system property 'record version'
	 */
	public static final String SYSTEM_VERSION = "@version";
	/**
	 * Name of system property 'record class'
	 */
	public static final String SYSTEM_CLASS = "@class";
	/**
	 * Mapping between entity field and system OrientDB field
	 */
	public static final Set<String> UNSUPPORTED_SYSTEM_FIELDS_IN_ENTITY = new HashSet<>( Arrays.asList( new String[]{ SYSTEM_VERSION } ) );
	/**
	 * Set of types that saved as binary
	 */
	public static final Set<Class> BASE64_TYPES;
	public static final String NULL_VALUE = "null";
	/**
	 * Set of system classes (tables)
	 */
	public static final Set<String> SYSTEM_CLASS_SET;
	/**
	 * Name of default sequence
	 */
	public static final String HIBERNATE_SEQUENCE = "hibernate_sequence";
	/**
	 * Name of table for using as table id generation
	 */
	public static final String HIBERNATE_SEQUENCE_TABLE = "sequences";

	public static final String PLOCAL_STORAGE_TYPE = "plocal";
	public static final String MEMORY_STORAGE_TYPE = "memory";
	public static final String GRAPTH_DATABASE_TYPE = "graph";
	public static final String DOCUMENT_DATABASE_TYPE = "document";

	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
	public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS z";

	public static final String GET_TABLE_SEQ_VALUE_FUNC = "getTableSeqValue".toUpperCase();
	@Deprecated
	public static final String EXECUTE_QUERY_FUNC = "executeQuery".toUpperCase();
	public static final String GET_NEXT_SEQ_VALUE_FUNC = "getNextSeqValue".toUpperCase();

	static {
		Set<String> set = new HashSet<>();
		set.add( SYSTEM_RID );
		set.add( SYSTEM_VERSION );
		set.add( SYSTEM_CLASS );
		SYSTEM_FIELDS = Collections.unmodifiableSet( set );

		Set<Class> set1 = new HashSet<>();
		set1.add( BigInteger.class );
		set1.add( byte[].class );
		BASE64_TYPES = Collections.unmodifiableSet( set1 );

		SYSTEM_CLASS_SET = Collections
				.unmodifiableSet( new HashSet<>( Arrays.asList( "V", "OSequence", "ORestricted", "OTriggered", "OIdentity", "ORole", "OSchedule",
						"OUser", "OFunction", "E" ) ) );

	}
}
