/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.engine.spi.TypedValue;

/**
 * Common base functionality for {@link QueryParserService} implementations.
 *
 * @author Gunnar Morling
 */
public abstract class BaseQueryParserService implements QueryParserService {

	/**
	 * Unwraps the given named parameters if they are wrapped into {@link TypedValue}s.
	 *
	 * @param namedParameters the original named parameters
	 * @return the unwrapped named parameters
	 */
	protected Map<String, Object> unwrap(Map<String, Object> namedParameters) {
		Map<String, Object> unwrapped = new HashMap<String, Object>( namedParameters.size() );

		for ( Entry<String, Object> entry : namedParameters.entrySet() ) {
			Object value = entry.getValue();
			unwrapped.put( entry.getKey(), value instanceof TypedValue ? ( (TypedValue) value ).getValue() : value );
		}

		return unwrapped;
	}
}
