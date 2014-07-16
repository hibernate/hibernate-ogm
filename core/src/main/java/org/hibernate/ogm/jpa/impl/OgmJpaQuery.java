/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.HibernateQuery;
import org.hibernate.jpa.internal.QueryImpl;
import org.hibernate.jpa.spi.AbstractEntityManagerImpl;

/**
 * Hibernate OGM implementation of both {@link HibernateQuery} and {@link TypedQuery}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class OgmJpaQuery<X> extends QueryImpl<X> implements HibernateQuery, TypedQuery<X> {

	public OgmJpaQuery(org.hibernate.Query query, EntityManager em) {
		super( query, convert( em ) );
	}

	private static AbstractEntityManagerImpl convert(EntityManager em) {
		if ( AbstractEntityManagerImpl.class.isInstance( em ) ) {
			return (AbstractEntityManagerImpl) em;
		}
		throw new IllegalStateException( String.format( "Unknown entity manager type [%s]", em.getClass().getName() ) );
	}
}
