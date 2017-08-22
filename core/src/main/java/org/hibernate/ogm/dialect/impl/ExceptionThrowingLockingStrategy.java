/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * A {@link LockingStrategy} which always raises an exception upon lock retrieval.
 * <p>
 * Used to initialize the locker infrastructure ORM while lazily raising an exception upon invocation of
 * {@code EntityManager#lock()} or similar.
 *
 * @author Gunnar Morling
 */
public class ExceptionThrowingLockingStrategy implements LockingStrategy {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private final Class<? extends GridDialect> gridDialectClass;
	private final LockMode lockMode;

	public ExceptionThrowingLockingStrategy(GridDialect gridDialect, LockMode lockMode) {
		this.gridDialectClass = gridDialect.getClass();
		this.lockMode = lockMode;
	}

	@Override
	public void lock(Serializable id, Object version, Object object, int timeout, SharedSessionContractImplementor session)
			throws StaleObjectStateException, LockingStrategyException {

		throw LOG.unsupportedLockMode( gridDialectClass, lockMode );
	}
}
