/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.document.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Captures useful data around the state of embeddable objects in tuples
 * Note that the current implementation is *not* specific to MongoDB on purpose.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class EmbeddableStateFinder {
	private final Tuple tuple;
	private final List<String> columns;
	private Set<String> nullEmbeddables = new HashSet<String>();
	private Map<String, String> columnToOuterMostNullEmbeddableCache = new HashMap<String, String>();

	public EmbeddableStateFinder(Tuple tuple, TupleContext tupleContext) {
		this.tuple = tuple;
		this.columns = tupleContext.getSelectableColumns();
	}

	/**
	 * Should only called on a column that is being set to null.
	 *
	 * Returns the most outer embeddable containing {@code column} that is entirely null.
	 * Return null otherwise i.e. not embeddable.
	 *
	 * The implementation lazily compute the embeddable state and caches it.
	 * The idea behind the lazy computation is that only some columns will be set to null
	 * and only in some situations.
	 * The idea behind caching is that an embeddable contains several columns, no need to recompute its state.
	 */
	public String getOuterMostNullEmbeddableIfAny(String column) {
		String[] path = column.split( "\\." );
		if ( !isEmbeddableColumn( path ) ) {
			return null;
		}
		// the cached value may be null hence the explicit key lookup
		if ( columnToOuterMostNullEmbeddableCache.containsKey( column ) ) {
			return columnToOuterMostNullEmbeddableCache.get( column );
		}
		return determineAndCacheOuterMostNullEmbeddable( column, path );
	}

	/**
	 * Walks from the most outer embeddable to the most inner one
	 * look for all columns contained in these embeddables
	 * and exclude the embeddables that have a non null column
	 * because of caching, the algorithm is only run once per column parameter
	 */
	private String determineAndCacheOuterMostNullEmbeddable(String column, String[] path) {
		String embeddable = path[0];
		// process each embeddable from less specific to most specific
		// exclude path leaves as it's a column and not an embeddable
		for ( int index = 0; index < path.length - 1; index++ ) {
			Set<String> columnsOfEmbeddable = getColumnsOfEmbeddableAndComputeEmbeddableNullness( embeddable );

			if ( nullEmbeddables.contains( embeddable ) ) {
				// the current embeddable only has null columns; cache that info for all the columns
				for ( String columnOfEmbeddable : columnsOfEmbeddable ) {
					columnToOuterMostNullEmbeddableCache.put( columnOfEmbeddable, embeddable );
				}
				break;
			}
			else {
				maybeCacheOnNonNullEmbeddable( path, index, columnsOfEmbeddable );
			}
			// a more specific null embeddable might be present, carry on
			embeddable += "." + path[index + 1];
		}
		return columnToOuterMostNullEmbeddableCache.get( column );
	}

	/**
	 * The embeddable is not null.
	 * Only cache the values if we are in the most specific embeddable containing {@code column}
	 * otherwise a more specific embeddable might be null and we would miss it
	 *
	 * Only set the values for the columns sharing this specific embeddable
	 * columns from deeper embeddables might be null
	 */
	private void maybeCacheOnNonNullEmbeddable(String[] path, int index, Set<String> columnsOfEmbeddable) {
		if ( index == path.length - 2 ) {
			//right level (i.e. the most specific embeddable for the column at bay
			for ( String columnInvolved : columnsOfEmbeddable ) {
				if ( columnInvolved.split( "\\." ).length == path.length ) {
					// Only cache for columns from the same embeddable
					columnToOuterMostNullEmbeddableCache.put( columnInvolved, null );
				}
			}
		}
	}

	/**
	 * Gets all the columns (direct and nested) of the given embeddable. Also manages the {@link #fullyNullEmbeddables}
	 * cache to avoid one additional loop over the columns.
	 */
	private Set<String> getColumnsOfEmbeddableAndComputeEmbeddableNullness(String embeddable) {
		Set<String> columnsOfEmbeddable = new HashSet<String>();
		boolean hasOnlyNullColumns = true;

		for ( String selectableColumn : columns ) {
			if ( !isColumnPartOfEmbeddable( embeddable, selectableColumn) ) {
				continue;
			}

			columnsOfEmbeddable.add( selectableColumn );

			if ( hasOnlyNullColumns && tuple.get( selectableColumn ) != null ) {
				hasOnlyNullColumns = false;
			}
		}

		if ( hasOnlyNullColumns ) {
			nullEmbeddables.add( embeddable );
		}

		return columnsOfEmbeddable;
	}

	private boolean isColumnPartOfEmbeddable(String embeddable, String selectableColumn) {
		return selectableColumn.startsWith( embeddable );
	}

	private boolean isEmbeddableColumn(String[] path) {
		return path.length >= 2;
	}
}
