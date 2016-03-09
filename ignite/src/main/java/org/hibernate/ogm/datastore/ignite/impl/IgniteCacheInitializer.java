/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;

public class IgniteCacheInitializer extends BaseSchemaDefiner {

	private static final long serialVersionUID = -8564869898957031491L;

	@Override
	public void initializeSchema(SchemaDefinitionContext context) {
		// if necessary, intercepts events cache initialization
	}

}
