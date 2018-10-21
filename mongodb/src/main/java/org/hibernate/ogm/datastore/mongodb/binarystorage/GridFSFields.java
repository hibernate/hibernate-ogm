/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.binarystorage;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks which fields of an entity have to be mapped using GridFS.
 *
 * @author Davide D'Alto
 */
public class GridFSFields {

	private final Class<?> entityClass;

	private Set<Field> gridfsFields = new HashSet();

	public GridFSFields(Class<?> entityClass) {
		this.entityClass = entityClass;
	}

	public void add(Field field, String binaryStorageType) {
		gridfsFields.add( field );
	}

	public Set<Field> getFields() {
		if ( gridfsFields.isEmpty() ) {
			return Collections.emptySet();
		}
		return gridfsFields;
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

}
