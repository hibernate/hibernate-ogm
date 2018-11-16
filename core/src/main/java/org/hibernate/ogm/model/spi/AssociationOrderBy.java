/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.spi;

import java.beans.IntrospectionException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.ReflectionHelper;

/**
 * Represents a single order by item of a given {@link javax.persistence.OrderBy}.
 * Expressed in term of datastore column name and order.
 *
 * @author Fabio Massimo Ercoli
 */
public class AssociationOrderBy implements Comparator<Map<String, Object>> {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	public enum Order {
		ASC(1), DESC(-1);
		protected int factor;

		Order(int factor) {
			this.factor = factor;
		}
	}

	private final String columnName;
	private final Order kind;

	public AssociationOrderBy(String expression, String order) {
		// extract column name from {} expression, such as "{columnName}"
		columnName = expression.substring( 1, expression.length() - 1 );
		kind = "desc".equalsIgnoreCase( order ) ? Order.DESC : Order.ASC;
	}

	public String getColumnName() {
		return columnName;
	}

	public Order getKind() {
		return kind;
	}

	@Override
	public int compare(Map<String, Object> o1Map, Map<String, Object> o2Map) {
		Object o1Value = o1Map.get( columnName );
		Object o2Value = o2Map.get( columnName );

		if ( o1Value == null && o2Value == null ) {
			return 0;
		}

		// put the null values at the end with default order
		if ( o1Value == null ) {
			return kind.factor;
		}
		if ( o2Value == null ) {
			return - kind.factor;
		}

		// try comparing values
		if ( !( o1Value instanceof Comparable ) || !( o2Value instanceof Comparable ) ) {
			return 0;
		}
		return ( (Comparable) o1Value ).compareTo( o2Value ) * kind.factor;
	}

	public static int compareWithOrderChain(Object o1, Object o2, List<AssociationOrderBy> orders) {
		Map<String, Object> o1Map;
		Map<String, Object> o2Map;

		try {
			o1Map = ReflectionHelper.introspect( o1 );
			o2Map = ReflectionHelper.introspect( o2 );
		}
		catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
			throw log.errorIntrospectingObject( e );
		}

		for ( AssociationOrderBy order : orders ) {
			int compare = order.compare( o1Map, o2Map );

			if ( compare != 0 ) {
				return compare;
			}
			// try next order in case of even
		}

		return 0;
	}
}
