/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.orientdb.utils;

import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Utility class for working with {@link Tuple}
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class TupleUtil {

	private static final Log log = LoggerFactory.getLogger();

	/**
	 * Convert {@link Tuple} to Map
	 *
	 * @param tuple tuple
	 * @return map
	 */
	public static Map<String, Object> toMap(Tuple tuple) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		for ( String columnName : tuple.getColumnNames() ) {
			map.put( columnName, tuple.get( columnName ) );
		}
		return map;
	}

}
