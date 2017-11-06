/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.datastore.mongodb.type.GeoPolygon;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;

/**
 * Persists {@link GeoPolygon} in the format expected by MongoDB.
 *
 * @author Guillaume Smet
 */
public class GeoPolygonGridType extends AbstractGenericBasicType<GeoPolygon>  {

	public static final GeoPolygonGridType INSTANCE = new GeoPolygonGridType();

	public GeoPolygonGridType() {
		super( GeoPolygonGridTypeDescriptor.INSTANCE, GeoPolygonTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "geopolygon";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
