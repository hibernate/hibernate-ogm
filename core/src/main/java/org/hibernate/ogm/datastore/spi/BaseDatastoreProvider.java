/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import org.hibernate.ogm.query.spi.QueryParserService;

/**
 * Recommended base class for {@link DatastoreProvider} implementations.
 *
 * @author Gunnar Morling
 *
 */
public abstract class BaseDatastoreProvider implements DatastoreProvider {

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return null;
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return BaseSchemaDefiner.class;
	}
}
