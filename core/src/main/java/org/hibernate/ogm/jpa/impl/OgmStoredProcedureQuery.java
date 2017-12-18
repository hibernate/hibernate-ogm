/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.hibernate.HibernateException;
import org.hibernate.jpa.internal.StoredProcedureQueryImpl;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;
import org.hibernate.ogm.storedprocedure.impl.NoSQLProcedureResultSetOutputImpl;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.result.ResultSetOutput;

/**
 * @author Davide D'Alto
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class OgmStoredProcedureQuery extends StoredProcedureQueryImpl {

	public OgmStoredProcedureQuery(ProcedureCall procedureCall, EntityManager entityManager) {
		super( procedureCall, convert( entityManager ) );
	}

	public OgmStoredProcedureQuery(ProcedureCallMemento procedureCallMemento, EntityManager entityManager) {
		super( procedureCallMemento, convert( entityManager ) );
	}

	private static HibernateEntityManagerImplementor convert(EntityManager em) {
		return em.unwrap( HibernateEntityManagerImplementor.class );
	}

	@Override
	public List getResultList() {
		try {
			final NoSQLProcedureResultSetOutputImpl rtn = (NoSQLProcedureResultSetOutputImpl) outputs().getCurrent();
			return rtn.getResultList();
		}
		catch (HibernateException he) {
			throw entityManager().convert( he );
		}
		catch (RuntimeException e) {
			entityManager().markForRollbackOnly();
			throw e;
		}
	}

	@Override
	public Object getSingleResult() {
		try {
			final ResultSetOutput rtn = (ResultSetOutput) outputs().getCurrent();
			return rtn.getSingleResult();
		}
		catch (HibernateException he) {
			throw entityManager().convert( he );
		}
		catch (RuntimeException e) {
			entityManager().markForRollbackOnly();
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public int getMaxResults() {
		int result = 0;
		for ( ParameterRegistration<?> parameterRegistration : (Set<ParameterRegistration<?>>) getParameters() ) {
			switch ( parameterRegistration.getMode() ) {
				case REF_CURSOR:
					result = 1;
					break;
				case OUT:
				case INOUT:
					throw new UnsupportedOperationException( "Out parameters not supported!" );
				default:
					break;
			}
		}
		return result;
	}

	@Override
	public Object getOutputParameterValue(int position) {
		throw new UnsupportedOperationException( "Out parameters not supported!" );
	}

	@Override
	public Object getOutputParameterValue(String parameterName) {
		throw new UnsupportedOperationException( "Out parameters not supported!" );
	}
}
