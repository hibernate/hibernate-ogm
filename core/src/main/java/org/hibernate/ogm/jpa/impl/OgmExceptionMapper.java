/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import javax.persistence.EntityExistsException;
import javax.transaction.SystemException;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.exception.EntityAlreadyExistsException;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ExceptionMapper;

/**
 * Maps OGM-specific exceptions, delegates other exceptions to ORM's own default mapper.
 *
 * @author Gunnar Morling
 */
public class OgmExceptionMapper implements ExceptionMapper {

	private final ExceptionMapper delegate;

	public OgmExceptionMapper(ExceptionMapper delegate) {
		this.delegate = delegate;
	}

	@Override
	public RuntimeException mapStatusCheckFailure(String message, SystemException systemException, SessionImplementor sessionImplementor) {
		return delegate.mapStatusCheckFailure( message, systemException, sessionImplementor );
	}

	@Override
	public RuntimeException mapManagedFlushFailure(String message, RuntimeException failure, SessionImplementor session) {
		// OGM-specific
		if ( EntityAlreadyExistsException.class.isInstance( failure ) ) {
			throw new EntityExistsException( failure );
		}
		// Let ORM deal with the others
		else {
			return delegate.mapManagedFlushFailure( message, failure, session );
		}
	}
}
