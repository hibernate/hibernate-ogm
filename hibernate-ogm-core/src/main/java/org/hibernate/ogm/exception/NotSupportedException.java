package org.hibernate.ogm.exception;

/**
 * Thrown when an operation is not supported by OGM
 * @author Emmanuel Bernard
 */
public class NotSupportedException extends RuntimeException {
	public NotSupportedException(String message) {
		super( message );
	}
}
