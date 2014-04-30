/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss;

import javax.ejb.Stateless;

/**
 * @author Davide D'Alto
 */
@Stateless
public class HelloWorldBean {

	public static final String HELLO = "Hello!";

	public String hello() {
		return HELLO;
	}

}
