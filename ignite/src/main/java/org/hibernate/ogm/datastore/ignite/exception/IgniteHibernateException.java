/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.exception;

import org.hibernate.HibernateException;

public class IgniteHibernateException extends HibernateException {

	private static final long serialVersionUID = 2965037850563431056L;

	public IgniteHibernateException(String message) {
		super( message );
	}
	public IgniteHibernateException(Throwable cause) {
		super( cause );
	}
	public IgniteHibernateException(String message, Throwable cause) {
		super( message, cause );
	}

}
