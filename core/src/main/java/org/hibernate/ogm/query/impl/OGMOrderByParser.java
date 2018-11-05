/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.model.spi.AssociationOrderBy;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.sql.ordering.antlr.ColumnMapper;
import org.hibernate.sql.ordering.antlr.GeneratedOrderByLexer;
import org.hibernate.sql.ordering.antlr.OrderByFragmentParser;
import org.hibernate.sql.ordering.antlr.TranslationContext;

import antlr.RecognitionException;

/**
 * Extracts the list of {@link AssociationOrderBy} from a JPQL order by fragment.
 * Applying the low level datastore column names.
 *
 * @author Fabio Massimo Ercoli
 */
public class OGMOrderByParser {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	public static List<AssociationOrderBy> parse(String fragment, SessionFactoryImplementor factory, ColumnMapper columnMapper) {

		TranslationContext context = new TranslationContext() {
			public SessionFactoryImplementor getSessionFactory() {
				return factory;
			}

			public Dialect getDialect() {
				return factory.getServiceRegistry().getService( JdbcServices.class ).getDialect();
			}

			public SQLFunctionRegistry getSqlFunctionRegistry() {
				return factory.getSqlFunctionRegistry();
			}

			public ColumnMapper getColumnMapper() {
				return columnMapper;
			}
		};

		GeneratedOrderByLexer lexer = new GeneratedOrderByLexer( new StringReader( fragment ) );

		// Perform the parsing (and some analysis/resolution).  Another important aspect is the collection
		// of "column references" which are important later to seek out replacement points in the
		// translated fragment.
		OrderByFragmentParser parser = new OrderByFragmentParser( lexer, context );
		try {
			parser.orderByFragment();
		}
		catch ( HibernateException e ) {
			throw e;
		}
		catch ( Throwable t ) {
			throw log.unableToParseOrderByFragment( fragment, t );
		}

		OGMOrderByRendered rendered = new OGMOrderByRendered();
		try {
			rendered.orderByFragment( parser.getAST() );
			return rendered.getOrderByItems();
		}
		catch (RecognitionException e) {
			throw log.unableToRenderOrderByFragment( fragment, e );
		}
	}
}
