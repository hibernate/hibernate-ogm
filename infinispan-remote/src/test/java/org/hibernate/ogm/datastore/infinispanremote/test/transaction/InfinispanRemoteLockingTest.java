/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.transaction;

import javax.persistence.PersistenceException;

import org.hibernate.ogm.backendtck.optimisticlocking.OptimisticLockingTest;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteServerRunner;

import org.junit.runner.RunWith;

/**
 * This test replaces the TCK base test {@link OptimisticLockingTest} for the current dialect.
 * In fact, at the moment, transactions over HotRod client must be PESSIMISTIC and must have REPEATABLE_READ as isolation level.
 *
 * The base test, in contrast, rely on READ_COMMITTED datastore upgraded to a psudo-REPEATABLE_READ granted by Hibernate OGM level.
 *
 * The behavior here is the same, with only exception of the expected exception class.
 *
 * @author Fabio Massimo Ercoli
 */
@RunWith(InfinispanRemoteServerRunner.class)
public class InfinispanRemoteLockingTest extends OptimisticLockingTest {

	public InfinispanRemoteLockingTest() {
		lockExceptionClass = PersistenceException.class;
	}
}
