package org.hibernate.ogm.datastore.ignite.exception;

import org.hibernate.dialect.lock.LockingStrategyException;

public class IgniteLockingStrategyException extends LockingStrategyException {

	private static final long serialVersionUID = -1163043836059135316L;

	public IgniteLockingStrategyException(Object entity, String message) {
		super(entity, message);
	}
	
	public IgniteLockingStrategyException(Object object, String message, Throwable cause){
		super(object, message, cause);
	}

}
