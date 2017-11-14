/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.Query;
import org.hibernate.jpa.HibernateQuery;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.internal.QueryImpl;
import org.hibernate.jpa.spi.AbstractEntityManagerImpl;
import org.hibernate.jpa.spi.AbstractQueryImpl;

/**
 * Hibernate OGM implementation of both {@link HibernateQuery} and {@link TypedQuery}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class OgmJpaQuery<X> extends QueryImpl<X> implements HibernateQuery, TypedQuery<X> {

	public OgmJpaQuery(org.hibernate.Query query, EntityManager em) {
		super( query, convert( em ) );
	}


	@Override
	public AbstractQueryImpl<X> setHint(String hintName, Object value) {
		AbstractQueryImpl<X> queryImpl = super.setHint( hintName, value );
		Map<String, Object> currentHints = queryImpl.getHints();
		if ( !currentHints.containsKey( hintName ) ) {
			// add hint
			currentHints.put( hintName, value );
		}
		return queryImpl;

	}

	@Override
	public Query getHibernateQuery() {
		org.hibernate.Query hibernateQuery = super.getHibernateQuery();
		// copy hints to hibernate query
		Map<String, Object> currentHints = getHints();
		if ( currentHints != null ) {
			for ( String hintName : currentHints.keySet() ) {
				if ( !isStandartHint( hintName ) ) {
					hibernateQuery.addQueryHint( hintName );
				}
			}
		}
		return hibernateQuery;
	}

	private boolean isStandartHint(String hintName) {
		return QueryHints.getDefinedHints().contains( hintName );
	}

	private static AbstractEntityManagerImpl convert(EntityManager em) {
		if ( AbstractEntityManagerImpl.class.isInstance( em ) ) {
			return (AbstractEntityManagerImpl) em;
		}
		throw new IllegalStateException( String.format( "Unknown entity manager type [%s]", em.getClass().getName() ) );
	}
}
