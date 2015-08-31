/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;


/**
 * Default implementation of {@link SchemaDefiner}. Specific implementations can override those hooks they're
 * interested in. Also provides utility methods useful for implementors.
 *
 * @author Gunnar Morling
 */
public class BaseSchemaDefiner implements SchemaDefiner {

	@Override
	public void validateMapping(SchemaDefinitionContext context) {
		// No-op
	}

	@Override
	public void initializeSchema(SchemaDefinitionContext context) {
		// No-op
	}
}
