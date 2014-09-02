/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.hibernate.ogm.test.integration.jboss.controller.MemberRegistration;
import org.hibernate.ogm.test.integration.jboss.model.Address;
import org.hibernate.ogm.test.integration.jboss.model.Member;
import org.junit.After;
import org.junit.Test;

/**
 * This class can be extended to execute this set of tests on the OGM module using different configurations or
 * deployments.
 *
 * @author Davide D'Alto
 */
public abstract class ModuleMemberRegistrationScenario {

	@Inject
	MemberRegistration memberRegistration;

	@After
	public void closeEntityManager() {
		memberRegistration.close();
	}

	@Test
	public void shouldGenerateAnId() throws Exception {
		Member newMember = memberRegistration.getNewMember();
		newMember.setName( "Davide D'Alto" );
		memberRegistration.register();

		assertNotNull( newMember.getId() );
	}

	@Test
	public void shouldFindPersistedMemberById() throws Exception {
		Member newMember = memberRegistration.getNewMember();
		newMember.setName( "Peter O'Tall" );
		newMember.getAddresses().add( new Address( "Mulholland Drive", "Los Angeles" ) );
		memberRegistration.register();

		Member found = memberRegistration.find( newMember.getId() );

		assertNotNull( "Expected at least one result after the indexing", found );
		assertEquals( "Search hasn't found a new member", newMember.getName(), found.getName() );
		assertEquals( "Member should have one address", 1, found.getAddresses().size() );
		assertEquals(
				"Member should have address with correct street",
				"Mulholland Drive",
				found.getAddresses().iterator().next().getStreet()
		);
	}

	@Test
	public void shouldFindPersistedMemberByIdWithQuery() throws Exception {
		Member newMember = memberRegistration.getNewMember();
		newMember.setName( "Peter O'Tall" );
		newMember.getAddresses().add( new Address( "Mulholland Drive", "Los Angeles" ) );
		memberRegistration.register();

		Member found = memberRegistration.findWithQuery( newMember.getId() );

		assertNotNull( "Expected at least one result using HQL", found );
		assertEquals( "HQL hasn't found a new member", newMember.getName(), found.getName() );
		assertEquals( "Member should have one address", 1, found.getAddresses().size() );
		assertEquals(
				"Member should have address with correct street",
				"Mulholland Drive",
				found.getAddresses().iterator().next().getStreet()
		);
	}

	@Test
	public void shouldBeAbleToFindMemberByEmail() throws Exception {
		Member newMember = memberRegistration.getNewMember();
		newMember.setName( "Sherlock Holmes" );
		newMember.setEmail( "SherlockHolmes@consultingdetective.co.uk" );
		newMember.getAddresses().add( new Address( "221B Baker St", "London" ) );
		memberRegistration.register();

		Member found = memberRegistration.findWithEmail( "she*" );

		assertNotNull( "Expected at least one result using Full text query", found );
		assertEquals( "Full text query hasn't found a new member", newMember.getName(), found.getName() );
		assertEquals( "Member should have one address", 1, found.getAddresses().size() );
		assertEquals(
				"Member should have address with correct street",
				"221B Baker St",
				found.getAddresses().iterator().next().getStreet()
		);
	}

	@Test
	public void shouldReturnNullWhenIdDoesNotExist() throws Exception {
		Member found = memberRegistration.find( -12L );

		assertNull( "Should return null when the id doesn't exist", found );
	}
}
