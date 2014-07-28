/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.QueryTranslatorImpl;
import org.hibernate.hql.spi.ParameterTranslations;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.Type;

/**
 * A {@link QueryTranslator} which delegates most of the work to the existing JP-QL parser implementation. Specifically,
 * all methods which only depend on the structure of the incoming JP-QL query are delegated. Only those methods
 * depending on the translated query (such as
 * {@link #list(org.hibernate.engine.spi.SessionImplementor, org.hibernate.engine.spi.QueryParameters)} are handled by
 * this class and its sub-classes. Over time, more and more methods should be implemented here rather than delegating
 * them.
 *
 * @author Gunnar Morling
 */
public abstract class LegacyParserBridgeQueryTranslator implements QueryTranslator {

	private static final Log log = LoggerFactory.make();

	protected final QueryTranslatorImpl delegate;

	public LegacyParserBridgeQueryTranslator(SessionFactoryImplementor sessionFactory, String queryIdentifier, String query, Map<?, ?> filters) {
		this.delegate = new QueryTranslatorImpl( queryIdentifier, query, filters, sessionFactory );
	}

	@Override
	public void compile(Map replacements, boolean shallow) throws QueryException, MappingException {
		try {
			// Need to prepare the delegate so getQuerySpaces() etc. canbe invoked on it
			delegate.compile( replacements, shallow );
		}
		catch ( Exception qse ) {
			throw log.querySyntaxException( qse, getQueryString() );
		}

		doCompile( replacements, shallow );
	}

	/**
	 * Compiles the given query so it can be executed several times with different parameter values.
	 */
	protected abstract void doCompile(Map replacements, boolean shallow) throws QueryException, MappingException;

	@Override
	public Set<Serializable> getQuerySpaces() {
		return delegate.getQuerySpaces();
	}

	@Override
	public String getQueryIdentifier() {
		return delegate.getQueryIdentifier();
	}

	@Override
	public String getSQLString() {
		return delegate.getSQLString();
	}

	@Override
	public List<String> collectSqlStrings() {
		return delegate.collectSqlStrings();
	}

	@Override
	public String getQueryString() {
		return delegate.getQueryString();
	}

	@Override
	public Map<?, ?> getEnabledFilters() {
		return delegate.getEnabledFilters();
	}

	@Override
	public Type[] getReturnTypes() {
		return delegate.getReturnTypes();
	}

	@Override
	public String[] getReturnAliases() {
		return delegate.getReturnAliases();
	}

	@Override
	public String[][] getColumnNames() {
		return delegate.getColumnNames();
	}

	@Override
	public ParameterTranslations getParameterTranslations() {
		return delegate.getParameterTranslations();
	}

	@Override
	public void validateScrollability() throws HibernateException {
		delegate.validateScrollability();
	}

	@Override
	public boolean containsCollectionFetches() {
		return delegate.containsCollectionFetches();
	}

	@Override
	public boolean isManipulationStatement() {
		return delegate.isManipulationStatement();
	}

	@Override
	public Class<?> getDynamicInstantiationResultType() {
		return delegate.getDynamicInstantiationResultType();
	}
}
