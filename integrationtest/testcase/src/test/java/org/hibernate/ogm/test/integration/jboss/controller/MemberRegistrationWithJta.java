/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss.controller;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.hibernate.ogm.test.integration.jboss.model.Member;

/**
 * A {@link MemberRegistration} bean that works when the transaction type of the persistence unit is set to JTA
 *
 * @author Davide D'Alto
 */
@Stateful
public class MemberRegistrationWithJta implements MemberRegistration {

	@Inject
	private EntityManager em;

	private Member newMember;

	@Override
	public Member getNewMember() {
		return newMember;
	}

	@Override
	public void register() {
		RegistrationExecutor.register( em, newMember );
		initNewMember();
	}

	@Override
	public Member find(Long id) {
		return RegistrationExecutor.find( em, id );
	}

	@Override
	public Member findWithNativeQuery(String nativeQuery) {
		return RegistrationExecutor.findWithNativeQuery( em, nativeQuery );
	}

	@Override
	public Member findWithQuery(Long id) {
		return RegistrationExecutor.findWithQuery( em, id );
	}

	@Override
	public Member findWithEmail(String email) {
		return RegistrationExecutor.findWithEmail( em, email );
	}

	@PostConstruct
	public void initNewMember() {
		newMember = new Member();
	}

	@Override
	public void close() {
	}

}
