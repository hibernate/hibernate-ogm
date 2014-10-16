/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;

/**
 * Useful functionality for handling grid dialects and their facets.
 * <p>
 * <b>Note:</b> Most dialect clients should obtain the facet they are interested in directly from the service registry
 * instead of using this class.
 * <p>
 * Dialect clients <b>must never</b> obtain facets by down-casting the current {@link GridDialect}, as dialect wrappers
 * such as {@link GridDialectLogger} need to implement all the possible dialect facets, also if the current dialect does
 * actually not support a specific facet such as {@link QueryableGridDialect}.
 *
 * @author Gunnar Morling
 */
class GridDialects {

	private GridDialects() {
	}

	/**
	 * Returns the given dialect, narrowed down to the given dialect facet in case it is implemented by the dialect.
	 *
	 * @param gridDialect the dialect of interest
	 * @param facetType the dialect facet type of interest
	 * @return the given dialect, narrowed down to the given dialect facet or {@code null} in case the given dialect
	 * does not implement the given facet
	 */
	static <T extends GridDialect> T getDialectFacetOrNull(GridDialect gridDialect, Class<T> facetType) {
		if ( hasFacet( gridDialect, facetType ) ) {
			@SuppressWarnings("unchecked")
			T asFacet = (T) gridDialect;
			return asFacet;
		}

		return null;
	}

	/**
	 * Whether the given grid dialect implements the specified facet or not.
	 *
	 * @param gridDialect the dialect of interest
	 * @param facetType the dialect facet type of interest
	 * @return {@code true} in case the given dialect implements the specified facet, {@code false} otherwise
	 */
	public static boolean hasFacet(GridDialect gridDialect, Class<? extends GridDialect> facetType) {
		if ( gridDialect instanceof ForwardingGridDialect ) {
			return hasFacet( ( (ForwardingGridDialect<?>) gridDialect ).getGridDialect(), facetType );
		}
		else {
			return facetType.isAssignableFrom( gridDialect.getClass() );
		}
	}
}
