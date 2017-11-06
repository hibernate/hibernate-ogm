/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.datastore.mongodb.type.GeoMultiPoint;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;

/**
 * Persists {@link GeoMultiPoint} in the format expected by MongoDB.
 *
 * @author Guillaume Smet
 */
public class GeoMultiPointGridType extends AbstractGenericBasicType<GeoMultiPoint>  {

	public static final GeoMultiPointGridType INSTANCE = new GeoMultiPointGridType();

	public GeoMultiPointGridType() {
		super( GeoMultiPointGridTypeDescriptor.INSTANCE, GeoMultiPointTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "geomultipoint";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
