/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.transaction.impl;

import static java.lang.Thread.currentThread;
import static java.lang.reflect.Proxy.newProxyInstance;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

public class WebSphereTransaction implements Transaction {
	private static final String ONE_PHASE_XA_RESOURCE_CLASS_NAME = "com.ibm.tx.jta.OnePhaseXAResource";
	private static volatile Class<?> onePhaseXAResourceClass;
	private final Transaction delegate;

	public WebSphereTransaction( final Transaction delegate ) {
		this.delegate = delegate;
		if ( onePhaseXAResourceClass == null ) {
			synchronized ( WebSphereTransaction.class ) {
				if ( onePhaseXAResourceClass == null ) {
					try {
						onePhaseXAResourceClass = Class.forName( ONE_PHASE_XA_RESOURCE_CLASS_NAME );
					}
					catch ( final ClassNotFoundException e ) {
						throw new RuntimeException( "Cannot instantiate Transaction" );
					}
				}
			}
		}
	}

	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
		delegate.commit();
	}

	@Override
	public boolean delistResource( final XAResource xaRes, final int flag ) throws IllegalStateException, SystemException {
		return delegate.delistResource( xaRes, flag );
	}

	@Override
	public boolean enlistResource( final XAResource xaResource ) throws RollbackException, IllegalStateException, SystemException {
		if ( xaResource == null ) {
			return false;
		}
		return delegate.enlistResource(
					(XAResource) newProxyInstance(
						currentThread().getContextClassLoader(),
						new Class<?>[] { onePhaseXAResourceClass }, new DelegatingInvocationHandler( xaResource )
					)
				);
	}

	@Override
	public int getStatus() throws SystemException {
		return delegate.getStatus();
	}

	@Override
	public void registerSynchronization( final javax.transaction.Synchronization sync ) throws RollbackException, IllegalStateException, SystemException {
		delegate.registerSynchronization( sync );
	}

	@Override
	public void rollback() throws IllegalStateException, SystemException {
		delegate.rollback();
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		delegate.setRollbackOnly();
	}
}
