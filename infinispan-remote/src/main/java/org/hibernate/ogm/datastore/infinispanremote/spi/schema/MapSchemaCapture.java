/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.spi.schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MapSchemaCapture implements SchemaCapture {

	private final Map<String,String> map = new HashMap<>();

	@Override
	public void put(String generatedProtobufName, String generatedProtoschema) {
		map.put( generatedProtobufName, generatedProtoschema );
	}

	public Map<String, String> asMap() {
		return Collections.unmodifiableMap( map );
	}

}
