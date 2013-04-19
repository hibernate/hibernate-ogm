/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.test.integration.jbossas7;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.hibernate.ogm.test.integration.jbossas7.controller.MemberRegistration;
import org.hibernate.ogm.test.integration.jbossas7.model.Member;
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
		memberRegistration.register();

		Member found = memberRegistration.find( newMember.getId() );

		assertNotNull( "Expected at least one result after the indexing", found );
		assertEquals( "Search hasn't found a new member", newMember.getName(), found.getName() );
	}

	@Test
	public void shouldReturnNullWhenIdDoesNotExist() throws Exception {
		Member found = memberRegistration.find( -12L );

		assertNull( "Should return null when the id doesn't exist", found );
	}
}
