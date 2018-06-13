/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.datastore.mongodb.type.GeoCollection;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;

/**
 * Persists {@link GeoCollection} in the format expected by MongoDB.
 *
 * @author Aleksandr Mylnkov
 */
public class GeoCollectionGridType extends AbstractGenericBasicType<GeoCollection> {

	public static final GeoCollectionGridType INSTANCE = new GeoCollectionGridType();

	public GeoCollectionGridType() {
		super( GeoCollectionGridTypeDescriptor.INSTANCE, GeoCollectionTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "geocollection";
	}
}
