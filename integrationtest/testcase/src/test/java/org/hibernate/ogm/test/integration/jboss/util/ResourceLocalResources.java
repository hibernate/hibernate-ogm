/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss.util;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

/**
 * This class uses CDI to alias Java EE resources, such as the persistence context, to CDI beans
 */
public class ResourceLocalResources {

	@PersistenceUnit
	private EntityManagerFactory factory;

	@Produces
	@Named("em")
	public EntityManager entityManager() {
		return factory.createEntityManager();
	}

}
