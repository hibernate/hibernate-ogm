/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.ogm.grid.EntityKeyMetadata;

/**
 * Contains all the {@link EntityKeyMetadata}s associated to an entity via *ToOne associations
 *
 * @author Davide D'Alto
 */
public class AssociatedEntitiesMetadata {

	public static final AssociatedEntitiesMetadata EMPTY_INSTANCE = new AssociatedEntitiesMetadata( Collections.<List<String>, EntityKeyMetadata>emptyMap(), Collections.<List<String>, String>emptyMap() );

	private final Map<List<String>, EntityKeyMetadata> associatedEntityKeyMetadata;
	private final Map<List<String>, String> roles;

	public static class Builder {

		private final Map<List<String>, EntityKeyMetadata> builderMap = new HashMap<List<String>, EntityKeyMetadata>();
		private final Map<List<String>, String> roles = new HashMap<List<String>, String>();

		public Builder add(String[] columns, EntityKeyMetadata entityKeyMetadata) {
			builderMap.put( Collections.unmodifiableList( Arrays.asList( columns ) ), entityKeyMetadata );
			return this;
		}

		public Builder add(String[] columns, String role) {
			roles.put( Collections.unmodifiableList( Arrays.asList( columns ) ), role );
			return this;
		}

		public AssociatedEntitiesMetadata build() {
			return new AssociatedEntitiesMetadata( builderMap, roles );
		}
	}

	private AssociatedEntitiesMetadata(Map<List<String>, EntityKeyMetadata> associatedEntityKeyMetadataMap, Map<List<String>, String> roles) {
		this.associatedEntityKeyMetadata = Collections.unmodifiableMap( associatedEntityKeyMetadataMap );
		this.roles = Collections.unmodifiableMap( roles );
	}

	/**
	 * Given a column that it is a foreign key, it will return the name of the table containing the associated entity
	 *
	 * @param column the column that it is (or it is part of) a foreign key
	 * @return table name of the associated entity or {@code null}
	 */
	public String getTargetEntityTable( String column ) {
		for ( Entry<List<String>, EntityKeyMetadata> entry : associatedEntityKeyMetadata.entrySet() ) {
			if ( entry.getKey().contains( column ) ) {
				return entry.getValue().getTable();
			}
		}
		return null;
	}

	/**
	 * Check if a column is part of o foreign key
	 *
	 * @param column the column to check
	 * @return {@code true} if the column is part of a foreign key, {@code false} otherwise
	 */
	public boolean isForeignKeyColumn(String column) {
		for ( List<String> key : associatedEntityKeyMetadata.keySet() ) {
			if ( key.contains( column ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Given a column part of a foreign key, it will return the corresponding column name on the associated entity
	 *
	 * @param column the column that it is a foreign key
	 * @return the corresponding column name on the associated entity or {@code null}
	 */
	public String getTargetColumnName(String column) {
		for ( Entry<List<String>, EntityKeyMetadata> entry : associatedEntityKeyMetadata.entrySet() ) {
			int index = entry.getKey().indexOf( column );
			if ( index != -1 ) {
				EntityKeyMetadata value = entry.getValue();
				return value.getColumnNames()[index];
			}
		}
		return null;
	}

	/**
	 * Given a column part of a foreign key, it will return the corresponding column name on the associated entity
	 *
	 * @param column the column that it is a foreign key
	 * @return the corresponding column name on the associated entity or {@code null}
	 */
	public String getRole(String column) {
		for ( Entry<List<String>, String> entry : roles.entrySet() ) {
			int index = entry.getKey().indexOf( column );
			if ( index != -1 ) {
				return entry.getValue();
			}
		}
		return null;
	}
}
