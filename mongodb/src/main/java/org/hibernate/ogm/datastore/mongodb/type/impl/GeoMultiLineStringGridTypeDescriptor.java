/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import org.hibernate.ogm.datastore.mongodb.type.GeoMultiLineString;

/**
 * Persists {@link GeoMultiLineString} in the format expected by MongoDB.
 *
 * @author Guillaume Smet
 */
public class GeoMultiLineStringGridTypeDescriptor extends AbstractGeoJsonObjectGridTypeDescriptor<GeoMultiLineString> {

	public static final GeoMultiLineStringGridTypeDescriptor INSTANCE = new GeoMultiLineStringGridTypeDescriptor();

	private GeoMultiLineStringGridTypeDescriptor() {
		super( GeoMultiLineString.class, GeoMultiLineString::fromDocument );
	}
}
