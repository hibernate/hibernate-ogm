/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.HibernateException;
import org.hibernate.QueryParameterException;
import org.hibernate.jpa.internal.StoredProcedureQueryImpl;
import org.hibernate.jpa.spi.AbstractEntityManagerImpl;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;
import org.hibernate.ogm.storedprocedure.impl.NoSQLProcedureCallImpl;
import org.hibernate.ogm.storedprocedure.impl.NoSQLProcedureOutputImpl;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.internal.ProcedureCallMementoImpl;
import org.hibernate.procedure.internal.ProcedureCallMementoImpl.ParameterMemento;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class OgmStoredProcedureQuery extends StoredProcedureQueryImpl {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );
	private Set<ParameterRegistration<?>> parameterRegistrations = new LinkedHashSet<>();
	private NoSQLProcedureCallImpl procedureCall;
	private HibernateEntityManagerImplementor entityManager;
	private ProcedureCallMementoImpl procedureCallMemento;

	public OgmStoredProcedureQuery(NoSQLProcedureCallImpl procedureCall, EntityManager entityManager) {
		super( procedureCall, convert( entityManager ) );
		this.procedureCall = procedureCall;
		this.entityManager = convert( entityManager );
	}

	public OgmStoredProcedureQuery(NoSQLProcedureCallImpl procedureCall, EntityManager entityManager, ProcedureCallMementoImpl procedureCallMemento) {
		this( procedureCall, entityManager );
		this.procedureCallMemento = procedureCallMemento;
		initProcedureCallByMemento();
		procedureCall.setMemento( procedureCallMemento );
	}

	private void initProcedureCallByMemento() {
		// register parameters
		for ( ParameterMemento parameterMemento : procedureCallMemento.getParameterDeclarations() ) {

			if ( procedureCall.getGridDialect().supportsNamedPosition() ) {
				// @todo supports named position!
			}
			else {
				registerStoredProcedureParameter( parameterMemento.getPosition() - 1, parameterMemento.getType(), parameterMemento.getMode() );
			}
		}
	}

	private static AbstractEntityManagerImpl convert(EntityManager em) {
		if ( AbstractEntityManagerImpl.class.isInstance( em ) ) {
			return (AbstractEntityManagerImpl) em;
		}
		throw new IllegalStateException( String.format( "Unknown entity manager type [%s]", em.getClass().getName() ) );
	}

	@SuppressWarnings("unchecked")
	@Override
	public StoredProcedureQuery registerStoredProcedureParameter(int position, Class type, ParameterMode mode) {
		entityManager().checkOpen( true );
		parameterRegistrations.add( procedureCall.registerParameter( position, type, mode ) );
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public StoredProcedureQuery registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode) {
		entityManager().checkOpen( true );
		parameterRegistrations.add( procedureCall.registerParameter( parameterName, type, mode ) );
		return this;
	}

	@Override
	public StoredProcedureQueryImpl setParameter(int position, Object value) {
		log.debugf( "set value %s for parameter index : %d", value, position );
		checkOpen( true );

		try {
			findNoSQLParameterRegistration( position ).bindValue( value );
		}
		catch (QueryParameterException e) {
			entityManager().markForRollbackOnly();
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (HibernateException he) {
			throw entityManager.convert( he );
		}

		return this;
	}

	private <X> ParameterRegistration<X> findNoSQLParameterRegistration(int parameterPosition) {
		if ( parameterRegistrations != null ) {
			for ( ParameterRegistration<?> param : parameterRegistrations ) {
				if ( param.getPosition() == null ) {
					continue;
				}
				if ( parameterPosition == param.getPosition() ) {
					return (ParameterRegistration<X>) param;
				}
			}
		}
		throw new IllegalArgumentException( "Parameter with that position [" + parameterPosition + "] did not exist" );
	}

	@Override
	public boolean execute() {
		return super.execute();
	}

	@Override
	public int getUpdateCount() {
		return super.getUpdateCount();
	}

	@Override
	public List getResultList() {
		try {
			final NoSQLProcedureOutputImpl rtn = (NoSQLProcedureOutputImpl) outputs().getCurrent();
			log.debugf( "getSynchronizedQuerySpaces: %s", this.procedureCall.getSynchronizedQuerySpaces() );

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
			final NoSQLProcedureOutputImpl rtn = (NoSQLProcedureOutputImpl) outputs().getCurrent();
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

	@Override
	public int getMaxResults() {
		// if have not ParameterMode.REF_CURSOR
		boolean hasOutParameters = false;
		for ( ParameterRegistration parameterRegistration : parameterRegistrations ) {
			// parameterRegistration.getMode().equals( ParameterMode.REF_CURSOR )
			if ( parameterRegistration.getMode().equals( ParameterMode.OUT ) ) {
				hasOutParameters = true;

			}
		}
		int result = 0;
		if ( hasOutParameters ) {

		}
		return result;
	}
}
