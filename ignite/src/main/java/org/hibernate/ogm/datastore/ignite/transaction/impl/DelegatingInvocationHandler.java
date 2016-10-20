/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.transaction.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class DelegatingInvocationHandler implements InvocationHandler {
	private final Object delegate;

	public DelegatingInvocationHandler( final Object delegate ) {
		this.delegate = delegate;
	}

	@Override
	public Object invoke( final Object proxy, final Method method, final Object[] args ) throws Throwable {
		return method.invoke( delegate, args );
	}
}
