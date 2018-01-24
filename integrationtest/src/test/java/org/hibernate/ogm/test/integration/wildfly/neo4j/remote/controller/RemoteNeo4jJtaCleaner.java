/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.wildfly.neo4j.remote.controller;

import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;

/**
 * @author Davide D'Alto
 */
@Singleton
@Startup
public class RemoteNeo4jJtaCleaner {

	@Inject
	private EntityManager em;

	@PreDestroy
	public void deleteAll() {
		em.createNativeQuery( "MATCH (n) DETACH DELETE (n)" ).executeUpdate();
	}
}
