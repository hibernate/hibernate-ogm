/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import javax.persistence.EntityManager;

import org.hibernate.jpa.internal.StoredProcedureQueryImpl;
import org.hibernate.jpa.spi.AbstractEntityManagerImpl;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class OgmJpaStoredProcedureQuery extends StoredProcedureQueryImpl {
	public OgmJpaStoredProcedureQuery(
			ProcedureCall procedureCall,
			EntityManager entityManager) {
		super( procedureCall, convert( entityManager ) );
	}

	public OgmJpaStoredProcedureQuery(
			ProcedureCallMemento memento,
			EntityManager entityManager) {
		super( memento, convert( entityManager ) );
	}

	private static AbstractEntityManagerImpl convert(EntityManager em) {
		if ( AbstractEntityManagerImpl.class.isInstance( em ) ) {
			return (AbstractEntityManagerImpl) em;
		}
		throw new IllegalStateException( String.format( "Unknown entity manager type [%s]", em.getClass().getName() ) );
	}
}
