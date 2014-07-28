/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import java.util.Map;

import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.spi.FilterTranslator;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.ogm.query.spi.QueryParserService;

/**
 * Creates {@link QueryTranslator}s. Depending on whether the underlying datastore supports queries itself, a translator
 * executing native queries or a translator executing Lucene queries via Hibernate Search will be created.
 *
 * @author Gunnar Morling
 */
public class OgmQueryTranslatorFactory implements QueryTranslatorFactory {

	@Override
	public QueryTranslator createQueryTranslator(String queryIdentifier, String queryString, Map filters, SessionFactoryImplementor factory,
			EntityGraphQueryHint entityGraphQueryHint) {

		QueryParserService queryParser = factory.getServiceRegistry().getService( QueryParserService.class );
		if ( queryParser != null ) {
			return new OgmQueryTranslator( factory, queryParser, queryIdentifier, queryString, filters );
		}
		else {
			return new FullTextSearchQueryTranslator( factory, queryIdentifier, queryString, filters );
		}
	}

	@Override
	public FilterTranslator createFilterTranslator(String queryIdentifier, String queryString, Map filters, SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException( "Not implemented" );
	}
}
