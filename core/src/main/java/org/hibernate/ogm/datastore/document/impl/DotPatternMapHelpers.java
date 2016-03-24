/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.impl;

import java.util.Map;
import java.util.regex.Pattern;

import org.hibernate.ogm.datastore.document.association.impl.DocumentHelpers;
import org.hibernate.ogm.datastore.document.options.MapStorageType;
import org.hibernate.ogm.datastore.document.options.spi.MapStorageOption;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;

/**
 * Provides functionality for dealing with (nested) fields of Map documents.
 *
 * @author Alan Fitton &lt;alan at eth0.org.uk&gt;
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 * @author Mark Paluch
 */
public class DotPatternMapHelpers {

	private static final Pattern DOT_SEPARATOR_PATTERN = Pattern.compile( "\\." );

	/**
	 * Remove a column from the Map
	 *
	 * @param entity the {@link Map} with the column
	 * @param column the column to remove
	 */
	public static void resetValue(Map<?, ?> entity, String column) {
		// fast path for non-embedded case
		if ( !column.contains( "." ) ) {
			entity.remove( column );
		}
		else {
			String[] path = DOT_SEPARATOR_PATTERN.split( column );
			Object field = entity;
			int size = path.length;
			for ( int index = 0; index < size; index++ ) {
				String node = path[index];
				Map parent = (Map) field;
				field = parent.get( node );
				if ( field == null && index < size - 1 ) {
					//TODO clean up the hierarchy of empty containers
					// no way to reach the leaf, nothing to do
					return;
				}
				if ( index == size - 1 ) {
					parent.remove( node );
				}
			}
		}
	}

	public static boolean hasField(Map entity, String dotPath) {
		return getValueOrNull( entity, dotPath ) != null;
	}

	public static <T> T getValueOrNull(Map entity, String dotPath, Class<T> type) {
		Object value = getValueOrNull( entity, dotPath );
		return type.isInstance( value ) ? type.cast( value ) : null;
	}

	public static Object getValueOrNull(Map entity, String dotPath) {
		// fast path for simple properties
		if ( !dotPath.contains( "." ) ) {
			return entity.get( dotPath );
		}

		String[] path = DOT_SEPARATOR_PATTERN.split( dotPath );
		int size = path.length;

		for ( int index = 0; index < size - 1; index++ ) {
			Object next = entity.get( path[index] );
			if ( next == null || !( next instanceof Map ) ) {
				return null;
			}
			entity = (Map) next;
		}

		String field = path[size - 1];
		return entity.get( field );
	}

	/**
	 * Links the two field names into a single left.right field name.
	 * If the left field is empty, right is returned
	 *
	 * @param left one field name
	 * @param right the other field name
	 *
	 * @return left.right or right if left is an empty string
	 */
	public static String flatten(String left, String right) {
		return left == null || left.isEmpty() ? right : left + "." + right;
	}

	/**
	 * Whether the rows of the given association should be stored in a hash using the single row key column as key or
	 * not.
	 */
	public static boolean organizeAssociationMapByRowKey(
			org.hibernate.ogm.model.spi.Association association,
			AssociationKey key,
			AssociationContext associationContext) {

		if ( association.isEmpty() ) {
			return false;
		}

		if ( key.getMetadata().getRowKeyIndexColumnNames().length != 1 ) {
			return false;
		}

		Object valueOfFirstRow = association.get( association.getKeys().iterator().next() )
				.get( key.getMetadata().getRowKeyIndexColumnNames()[0] );

		if ( !( valueOfFirstRow instanceof String ) ) {
			return false;
		}

		// The list style may be explicitly enforced for compatibility reasons
		return getMapStorage( associationContext ) == MapStorageType.BY_KEY;
	}

	private static MapStorageType getMapStorage(AssociationContext associationContext) {
		return associationContext.getAssociationTypeContext().getOptionsContext().getUnique( MapStorageOption.class );
	}

	/**
	 * Returns the shared prefix with a trailing dot (.) of the association columns or {@literal null} if there is no shared prefix.
	 *
	 * @param associationKey the association key
	 *
	 * @return the shared prefix with a trailing dot (.) of the association columns or {@literal null}
	 */
	public static String getColumnSharedPrefixOfAssociatedEntityLink(AssociationKey associationKey) {
		String[] associationKeyColumns = associationKey.getMetadata()
				.getAssociatedEntityKeyMetadata()
				.getAssociationKeyColumns();
		// we used to check that columns are the same (in an ordered fashion)
		// but to handle List and Map and store indexes / keys at the same level as the id columns
		// this check is removed
		String prefix = DocumentHelpers.getColumnSharedPrefix( associationKeyColumns );
		return prefix == null ? "" : prefix + ".";
	}
}
