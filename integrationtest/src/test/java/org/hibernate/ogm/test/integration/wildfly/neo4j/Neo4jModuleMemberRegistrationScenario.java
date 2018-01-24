/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.wildfly.neo4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.annotation.PreDestroy;

import org.hibernate.ogm.test.integration.wildfly.testcase.ModuleMemberRegistrationScenario;
import org.hibernate.ogm.test.integration.wildfly.testcase.model.Member;
import org.junit.Test;
/**
 * @author Davide D'Alto
 */
public class Neo4jModuleMemberRegistrationScenario extends ModuleMemberRegistrationScenario {

	@PreDestroy
	public void deleteAll() {
	}

	@Test
	public void shouldFindPersistedMemberByIdWithNativeQuery() throws Exception {
		Member newMember = memberRegistration.getNewMember();
		newMember.setName( "Giovanni Doe" );
		memberRegistration.register();

		String nativeQuery = "MATCH (n:Member {id: " + newMember.getId() + "}) RETURN n";
		Member found = memberRegistration.findWithNativeQuery( nativeQuery );

		assertNotNull( "Expected at least one result using a native query", found );
		assertEquals( "Native query hasn't found a new member", newMember.getName(), found.getName() );
	}
}
