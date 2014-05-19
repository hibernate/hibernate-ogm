/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.mapping.model;

import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.service.impl.QueryParserService;

/**
 * @author Gunnar Morling
 */
public class SampleDatastoreProvider implements DatastoreProvider {

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		throw new UnsupportedOperationException();
	}
}
