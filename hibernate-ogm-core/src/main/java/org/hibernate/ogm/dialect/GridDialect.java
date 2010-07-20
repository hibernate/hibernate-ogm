package org.hibernate.ogm.dialect;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.persister.entity.Lockable;

/**
 * Dialect abstracting Hibernate OGM from the grid implementation
 *
 * @author Emmanuel Bernard
 */
public interface GridDialect {
	LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode);
}
