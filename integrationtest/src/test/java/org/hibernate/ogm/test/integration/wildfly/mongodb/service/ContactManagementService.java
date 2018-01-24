/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.wildfly.mongodb.service;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.hibernate.ogm.test.integration.wildfly.mongodb.model.EmailAddress;
import org.hibernate.ogm.test.integration.wildfly.mongodb.model.PhoneNumber;

/**
 * @author Gunnar Morling
 */
@Stateless
public class ContactManagementService {

	@Inject
	private EntityManager entityManager;

	public void persistContacts(List<PhoneNumber> phoneNumbers, List<EmailAddress> emailAddresses) {
		for ( PhoneNumber phoneNumber : phoneNumbers ) {
			entityManager.persist( phoneNumber );
		}

		for ( EmailAddress emailAddress : emailAddresses ) {
			entityManager.persist( emailAddress );
		}
	}
}
