/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.datastore.document.association.impl.DocumentHelpers;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationType;

/**
 * Helpers to transform associative data into association rows.
 * @author Mark Paluch
 */
public class MapAssociationRowsHelpers {

	private static final String EMBEDDABLE_COLUMN_PREFIX = ".value.";
	private static final String PATH_SEPARATOR = ".";

	/**
	 * Transform {@code toManyValue} into association rows
	 * @param toManyValue an object, {@link Collection} or a {@link Map}, can be {@literal null}
	 * @param associationKey the association key
	 * @return collection of association rows. Empty list if {@code toManyValue} is null
	 */
	public static Collection<?> getRows(
			Object toManyValue,
			AssociationKey associationKey) {
		Collection<?> rows = null;

		if ( associationKey.getMetadata().getAssociationType() == AssociationType.ONE_TO_ONE ) {
			Object oneToOneValue = toManyValue;
			if ( oneToOneValue != null ) {
				rows = Collections.singletonList( oneToOneValue );
			}
		}

		// list of rows
		if ( toManyValue instanceof Collection ) {
			rows = (Collection<?>) toManyValue;
		}
		// a map-typed association, rows are organized by row key
		else if ( toManyValue instanceof Map ) {
			rows = getRowsFromMapAssociation( associationKey, (Map) toManyValue );
		}

		return rows != null ? rows : Collections.emptyList();
	}

	/**
	 * Restores the list representation of the given map-typed association. E.g. { 'home' : 123, 'work' : 456 } will be
	 * transformed into [{ 'addressType='home', 'address_id'=123}, { 'addressType='work', 'address_id'=456} ]) as
	 * expected by the row accessor.
	 */
	private static Collection<Map<String, Object>> getRowsFromMapAssociation(
			AssociationKey associationKey,
			Map<String, Object> value) {
		String rowKeyIndexColumn = associationKey.getMetadata().getRowKeyIndexColumnNames()[0];
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

		String[] associationKeyColumns = associationKey.getMetadata()
				.getAssociatedEntityKeyMetadata()
				.getAssociationKeyColumns();

		// Omit shared prefix of compound ids, will be handled in the row accessor
		String prefix = DocumentHelpers.getColumnSharedPrefix( associationKeyColumns );
		prefix = prefix == null ? "" : prefix + ".";

		String embeddedValueColumnPrefix = associationKey.getMetadata().getCollectionRole() + EMBEDDABLE_COLUMN_PREFIX;

		// restore the list representation
		for ( String rowKey : value.keySet() ) {
			Object mapRow = value.get( rowKey );

			// include the row key index column
			Map<String, Object> row = new HashMap<>();
			set( row, rowKeyIndexColumn, rowKey );

			// several value columns, copy them all
			if ( mapRow instanceof Map ) {
				for ( String column : associationKey.getMetadata()
						.getAssociatedEntityKeyMetadata()
						.getAssociationKeyColumns() ) {
					// The column is part of an element collection; Restore the "value" node in the hierarchy
					if ( column.startsWith( embeddedValueColumnPrefix ) ) {
						set(
								row,
								column.substring( associationKey.getMetadata().getCollectionRole().length() + 1 ),
								( (Map) mapRow ).get( column.substring( embeddedValueColumnPrefix.length() ) )
						);
					}
					else {
						set(
								row,
								column.substring( prefix.length() ),
								( (Map) mapRow ).get( column.substring( prefix.length() ) )
						);

					}
				}
			}
			else {
				// single value column
				set(
						row,
						associationKey.getMetadata().getAssociatedEntityKeyMetadata().getAssociationKeyColumns()[0],
						mapRow
				);
			}

			rows.add( row );

		}

		return rows;
	}

	/**
	 * Set a key/value within the {@code target} map. Nested maps will be converted into single properties using the path separator.
	 *
	 * @param name the transport map
	 * @param name the property name
	 * @param value the property value
	 */
	@SuppressWarnings("unchecked")
	public static void set(Map<String, Object> target, String name, Object value) {
		if ( value instanceof Map ) {
			setMapValue( target, name, (Map<String, Object>) value );
		}
		else {
			target.put( name, value );
		}
	}

	/**
	 * Saves each entry of the map as a single property using the path separator.
	 * <p>
	 * For example { k1 = { k11 = v11, k12 = v12 } } becomes { k1.k11=v11, k1.k12=v12 }.
	 */
	private static void setMapValue(Map<String, Object> target, String name, Map<String, Object> value) {
		for ( Map.Entry<String, Object> entry : value.entrySet() ) {
			set( target, name + PATH_SEPARATOR + entry.getKey(), entry.getValue() );
		}
	}
}
