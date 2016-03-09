/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.impl;

import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.query.impl.OgmQueryTranslator;
import org.hibernate.ogm.query.spi.QueryParserService;

/**
 * Ignite-specific extension of {@link OgmQueryTranslator}}
 *
 * @author Dmitriy Kozlov
 *
 */
public class IgniteQueryTranslatorImpl extends OgmQueryTranslator {

	public IgniteQueryTranslatorImpl(SessionFactoryImplementor sessionFactory,
			QueryParserService queryParser, String queryIdentifier,
			String query, Map<?, ?> filters) {
		super( sessionFactory, queryParser, queryIdentifier, query, filters );
	}

}
