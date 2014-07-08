/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.spi.SchemaDefiner;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.service.Service;

/**
 * Provides datastore centric configurations and native access.
 * <p>
 * Implementations of this service offer native interfaces to access the underlying datastore. It is also responsible
 * for starting and stopping the connection to the datastore.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public interface DatastoreProvider extends Service {

	Class<? extends GridDialect> getDefaultDialect();

	/**
	 * Returns the type of {@link QueryParserService} to be used for executing queries against the underlying datastore.
	 *
	 * @return the default {@link QueryParserService} for the underlying datastore; never {@code null}
	 */
	Class<? extends QueryParserService> getDefaultQueryParserServiceType();

	/**
	 * Returns the type of the {@link SchemaDefiner} of this datastore. An instance of this type will be added to the
	 * service registry using the {@link SchemaDefiner} service role.
	 *
	 * @return the schema definer type
	 */
	Class<? extends SchemaDefiner> getSchemaDefinerType();
}
