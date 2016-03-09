/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.exception;

import org.hibernate.dialect.lock.LockingStrategyException;

public class IgniteLockingStrategyException extends LockingStrategyException {

	private static final long serialVersionUID = -1163043836059135316L;

	public IgniteLockingStrategyException(Object entity, String message) {
		super( entity, message );
	}

	public IgniteLockingStrategyException(Object object, String message, Throwable cause) {
		super( object, message, cause );
	}

}
