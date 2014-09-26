/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss.service;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.hibernate.ogm.test.integration.jboss.model.PhoneNumber;

/**
 * @author Gunnar Morling
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
		List<PhoneNumber> result = entityManager.createQuery( "from PhoneNumber pn where name = :name", PhoneNumber.class ).setParameter( "name", name ).getResultList();
		return result.isEmpty() ? null : result.iterator().next();
	}
}
