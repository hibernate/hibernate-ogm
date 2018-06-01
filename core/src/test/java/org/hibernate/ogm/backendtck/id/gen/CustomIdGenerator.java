/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id.gen;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class CustomIdGenerator implements IdentifierGenerator {

	static final String ID_PREFIX = "singleVMUniqueId-";
	private AtomicInteger threadSafeCounter = new AtomicInteger( 0 );

	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		return ID_PREFIX + threadSafeCounter.incrementAndGet();
	}
}
