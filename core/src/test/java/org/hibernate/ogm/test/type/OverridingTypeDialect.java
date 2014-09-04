/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.type;

import java.util.UUID;

import org.hibernate.ogm.datastore.map.impl.MapDatastoreProvider;
import org.hibernate.ogm.datastore.map.impl.MapDialect;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
* @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
*/
public class OverridingTypeDialect extends MapDialect {

	public OverridingTypeDialect(MapDatastoreProvider provider) {
		super( provider );
	}

	@Override
	public GridType overrideType(Type type) {
		//all UUID properties are mapped with exploding type
		if ( UUID.class.equals( type.getReturnedClass() ) ) {
			return ExplodingType.INSTANCE;
		}
		//timestamp and time mapping are ignored, only raw dates are handled
		if ( type == StandardBasicTypes.DATE ) {
			return CustomDateType.INSTANCE;
		}
		return null;
	}
}
