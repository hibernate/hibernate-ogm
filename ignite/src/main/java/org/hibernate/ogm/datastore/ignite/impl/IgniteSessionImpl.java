/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.datastore.ignite.loader.criteria.OgmCriteriaLoader;
import org.hibernate.ogm.datastore.ignite.loader.criteria.impl.CriteriaCustomQuery;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionImpl;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

public class IgniteSessionImpl extends OgmSessionImpl {

	private static final long serialVersionUID = 5211320997926056205L;

	public IgniteSessionImpl(OgmSessionFactory factory, EventSource delegate) {
		super( factory, delegate );
	}

	@Override
	public Criteria createCriteria(Class persistentClass) {
		errorIfClosed();
		checkTransactionSynchStatus();
		return new CriteriaImpl(persistentClass.getName(), persistentClass.getSimpleName(), this);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Criteria createCriteria(Class persistentClass, String alias) {
		errorIfClosed();
		checkTransactionSynchStatus();
		return new CriteriaImpl( persistentClass.getName(), alias, this );
	}

	@Override
	public Criteria createCriteria(String entityName) {
		throw new NotSupportedException( "OGM-23", "Criteria queries with entityName are not supported. Use createCriteria(Class persistanseClass))" );
	}

	@Override
	public Criteria createCriteria(String entityName, String alias) {
		throw new NotSupportedException( "OGM-23", "Criteria queries with entityName are not supported. Use createCriteria(Class persistanseClass))" );
	}

	@Override
	public List<?> list(Criteria criteria) throws HibernateException {
		CriteriaImpl criteriaImpl = (CriteriaImpl) criteria;

		if (!criteriaImpl.getLockModes().isEmpty()) {
			throw new HibernateException("Criteria queries with LockModes not supported");
		}

		errorIfClosed();
		checkTransactionSynchStatus();

		CriteriaCustomQuery query = new CriteriaCustomQuery(criteriaImpl, getFactory(), getLoadQueryInfluencers(), this);
		OgmCriteriaLoader loader = new OgmCriteriaLoader(query, getFactory());

		return loader.list( getDelegate(), null );
	}

	// Copied from org.hibernate.internal.SessionImpl.checkTransactionSynchStatus() to mimic same behaviour
	private void checkTransactionSynchStatus() {
		pulseTransactionCoordinator();
		delayedAfterCompletion();
	}

	// Copied from org.hibernate.internal.SessionImpl.pulseTransactionCoordinator() to mimic same behaviour
	private void pulseTransactionCoordinator() {
		if ( !isClosed() ) {
			getDelegate().getTransactionCoordinator().pulse();
		}
	}

	// Copied from org.hibernate.internal.SessionImpl.delayedAfterCompletion() to mimic same behaviour
	private void delayedAfterCompletion() {
		TransactionCoordinator transactionCoordinator = getDelegate().getTransactionCoordinator();
		if ( transactionCoordinator instanceof JtaTransactionCoordinatorImpl ) {
			( (JtaTransactionCoordinatorImpl) transactionCoordinator ).getSynchronizationCallbackCoordinator()
					.processAnyDelayedAfterCompletion();
		}
	}

}
