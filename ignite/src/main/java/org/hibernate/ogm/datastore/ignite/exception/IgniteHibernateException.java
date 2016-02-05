package org.hibernate.ogm.datastore.ignite.exception;

import org.hibernate.HibernateException;

public class IgniteHibernateException extends HibernateException {

	private static final long serialVersionUID = 2965037850563431056L;

	public IgniteHibernateException(String message) {
		super(message);
	}
	
	public IgniteHibernateException(Throwable cause){
		super(cause);
	}
	
	public IgniteHibernateException(String message, Throwable cause){
		super(message, cause);
	}

}
