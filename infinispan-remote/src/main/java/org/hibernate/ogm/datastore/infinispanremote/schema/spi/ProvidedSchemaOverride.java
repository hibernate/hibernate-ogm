/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.schema.spi;

public final class ProvidedSchemaOverride implements SchemaOverride {

	private final String schema;

	public ProvidedSchemaOverride(String schema) {
		this.schema = schema;
	}

	@Override
	public String createProtobufSchema() {
		return schema;
	}

}
