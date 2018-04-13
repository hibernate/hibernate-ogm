/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.util.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.hibernate.ogm.model.spi.Tuple;

/**
 * Helper class containing utility methods for converting Java objects to Tuples.
 *
 * @author The Viet Nguyen &amp;ntviet18@gmail.com&amp;
 */
public class TupleExtractor {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private TupleExtractor() {
		// util class
	}

	/**
	 * <p>
	 * Extract {@link Tuple} values from given object. If the given object is an {@link Iterable} a list of tuples extracted from its
	 * elements will be returned. Otherwise, a single element list with a {@link Tuple} of key 'result' and value of 'obj' itself will
	 * be returned.
	 * </p>
	 * <p>
	 * If given object is an {@link Iterable}, this method also try to determine its element type. If the element is a
	 * {@link Map}, a {@link Tuple} with equivalent key/value will be extracted. Otherwise, element is treated as a POJO
	 * and introspected values will be returned.
	 * </p>
	 * <p>
	 * Examples:
	 * <pre>{@code
	 *       // equivalent to List [ Tuple( key, value ) ]
	 *       TupleExtractor.extractTuplesFromObject( singleton( singletonMap( "key", "value" ) ) );
	 *       // equivalent to List [ Tuple( key, value ) ]
	 *       TupleExtractor.extractTuplesFromObject( singleton( new Subject( "key", "value" ) ) );
	 *       // equivalent to List [ Tuple( result, 1 ) ]
	 *       TupleExtractor.extractTuplesFromObject( 1 );
	 *     }</pre>
	 * </p>
	 *
	 * @param obj object to be extracted
	 *
	 * @return list tuple result
	 */
	public static List<Tuple> extractTuplesFromObject(Object obj) {
		if ( obj instanceof Iterable ) {
			Iterable<?> it = (Iterable) obj;
			return StreamSupport.stream( it.spliterator(), false )
					.map( TupleExtractor::extractFromObject )
					.collect( Collectors.toList() );
		}
		Tuple tuple = new Tuple();
		tuple.put( "result", obj );
		return Collections.singletonList( tuple );
	}

	private static Tuple extractFromObject(Object obj) {
		if ( obj instanceof Map ) {
			return extractFromMap( ( (Map) obj ) );
		}
		try {
			return extractFromPojo( obj );
		}
		catch (Exception e) {
			throw log.cannotExtractTupleFromObject( obj, e );
		}
	}

	private static Tuple extractFromMap(Map<?, ?> map) {
		Tuple tuple = new Tuple();
		for ( Map.Entry<?, ?> e : map.entrySet() ) {
			tuple.put( String.valueOf( e.getKey() ), e.getValue() );
		}
		return tuple;
	}

	private static Tuple extractFromPojo(Object obj) throws Exception {
		Tuple tuple = new Tuple();
		Map<String, Object> introspect = ReflectionHelper.introspect( obj );
		introspect.forEach( tuple::put );
		return tuple;
	}
}
