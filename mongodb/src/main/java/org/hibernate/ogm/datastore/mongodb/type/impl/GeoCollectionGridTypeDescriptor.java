/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import org.hibernate.ogm.datastore.mongodb.type.GeoCollection;

/**
 * Persists {@link GeoCollection} in the format expected by MongoDB.
 *
 * @author Aleksandr Mylnikov
 */
public class GeoCollectionGridTypeDescriptor extends AbstractGeoJsonObjectGridTypeDescriptor<GeoCollection> {

	public static final GeoCollectionGridTypeDescriptor INSTANCE = new GeoCollectionGridTypeDescriptor();

	protected GeoCollectionGridTypeDescriptor() {
		super( GeoCollection.class, GeoCollection::fromDocument );
	}
}
