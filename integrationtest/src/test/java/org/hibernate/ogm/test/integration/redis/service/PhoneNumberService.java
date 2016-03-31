/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.redis.service;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.hibernate.ogm.test.integration.redis.model.PhoneNumber;

/**
 * @author Mark Paluch
 */
@Stateless
public class PhoneNumberService {

	@Inject
	private EntityManager entityManager;

	public PhoneNumber createPhoneNumber(String name, String value) {
		PhoneNumber phoneNumber = new PhoneNumber( name, value );
		entityManager.persist( phoneNumber );
		return phoneNumber;
	}

	public PhoneNumber getPhoneNumber(String name) {
		return entityManager.find(
				PhoneNumber.class, name
		);
	}

	public void deletePhoneNumber(String name) {
		entityManager.remove( getPhoneNumber( name ) );
	}
}
