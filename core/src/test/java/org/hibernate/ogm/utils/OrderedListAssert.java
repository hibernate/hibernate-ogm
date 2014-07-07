/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import static java.util.Collections.emptyList;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fest.assertions.Assertions;
import org.fest.assertions.ListAssert;

/**
 * Provide additional assert methods to {@link ListAssert}
 *
 * @author Davide D'Alto
 */
public class OrderedListAssert extends ListAssert {

	private final boolean ignoringNullPosition;

	protected OrderedListAssert(List<?> actual) {
		this( actual, false );
	}

	private OrderedListAssert(List<?> actual, boolean ignoreNullPosition) {
		super( actual );
		this.ignoringNullPosition = ignoreNullPosition;
	}

	public OrderedListAssert ignoreNullOrder() {
		return new OrderedListAssert( actual, true );
	}

	@Override
	public OrderedListAssert onProperty(String propertyName) {
		isNotNull();
		if ( actual.isEmpty() ) {
			return new OrderedListAssert( emptyList() );
		}
		return new OrderedListAssert( getProjection( actual, propertyName ) );
	}

	/**
	 * This methods behave like {@link ListAssert#containsExactly(Object...)} with the exception that the order of the
	 * null elements is ignored if {@link OrderedListAssert#ignoreNullOrder()} is called.
	 * Note that in this last case, the test will fail if nulls are both at the end and at the beginning.
	 *
	 * @see ListAssert#containsExactly(Object...)
	 */
	@Override
	public OrderedListAssert containsExactly(Object... expected) {
		if ( ignoringNullPosition ) {
			Assertions.assertThat( actual ).hasSize( expected.length );
			List<Object> actualWithoutNull = trimNull( actual );
			List<Object> expectedWithoutNull = trimNull( Arrays.asList( expected ) );
			Assertions.assertThat( actualWithoutNull ).isEqualTo( expectedWithoutNull );
			if ( actual.size() > 1 && actualWithoutNull.size() > 0 ) {
				if ( actual.get( 0 ) == null && actual.get( actual.size() - 1 ) == null ) {
					throw failure( "Null order should be consistent: " + actual );
				}
			}
		}
		else {
			super.containsExactly( expected );
		}
		return this;
	}

	/**
	 * Remove null values at the beginning and at the end of a list
	 */
	private static List<Object> trimNull(List<?> original) {
		List<Object> trimmed = new ArrayList<Object>();
		int last = findLastNonNullValuePosition( original );
		int i = -1;
		boolean skipNulls = true;
		while ( i < last ) {
			Object object = original.get( ++i );
			if ( !skipNulls ) {
				trimmed.add( object );
			}
			else if ( skipNulls && object != null ) {
				skipNulls = false;
				trimmed.add( object );
			}
		}
		return trimmed;
	}

	private static int findLastNonNullValuePosition(List<?> actualWithNull) {
		int last = actualWithNull.size() - 1;
		while ( last > 0 ) {
			if ( actualWithNull.get( last ) != null ) {
				return last;
			}
			last--;
		}
		return -1;
	}

	private List<Object> getProjection(List<?> source, String propertyName) {
		List<Object> projection = new ArrayList<Object>( source.size() );
		for ( Object object : source ) {
			projection.add( getPropertyValue( object, propertyName ) );
		}
		return projection;
	}

	private Object getPropertyValue(Object object, String propertyName) {
		try {
			BeanInfo info = Introspector.getBeanInfo( object.getClass() );
			PropertyDescriptor[] properties = info.getPropertyDescriptors();
			for ( PropertyDescriptor property : properties ) {
				if ( property.getName().equals( propertyName ) ) {
					return property.getReadMethod().invoke( object );
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}

		throw new IllegalArgumentException( "No property " + propertyName + " exists in " + object.getClass() );
	}
}
