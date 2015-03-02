/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.cfg.impl;

import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.jpa.HibernateOgmPersistence;
import org.hibernate.ogm.options.spi.OptionsService.OptionsServiceContext;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.service.impl.OgmIntegrator;

/**
 * Constants used within OGM, not intended for public use.
 *
 * @author Gunnar Morling
 */
public class InternalProperties {

	/**
	 * Used to make sure that Hibernate OGM's custom service implementations are only registered with the bootstrap
	 * process if the persistence unit in question uses Hibernate OGM. For that purpose, this property is set to
	 * {@code true} in {@link OgmConfiguration} and {@link HibernateOgmPersistence}. If Hibernate ORM is bootstrapped,
	 * {@link OgmIntegrator} will still be executed (as it is discovered via the Java service loader) but it will take
	 * no effect since this property is not set in this case.
	 *
	 * @see OgmIntegrator
	 */
	public static final String OGM_ON = "hibernate.ogm._activate";

	/**
	 * Name of the configuration option for passing in set up {@link OptionsServiceContext}s.
	 */
	public static final String OGM_OPTION_CONTEXT = "hibernate.ogm.options.context";

	/**
	 * Name of the configuration option for specifying the {@link QueryParserService} to be used. Accepts a
	 * fully-qualified class name. If not specified, the parser type returned by
	 * {@link DatastoreProvider#getDefaultQueryParserServiceType()} will be used.
	 */
	public static final String QUERY_PARSER_SERVICE = "hibernate.ogm.query.parser";

	private InternalProperties() {
	}
}
