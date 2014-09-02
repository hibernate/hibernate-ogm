/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss.controller;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.ogm.test.integration.jboss.model.Member;

/**
 * A {@link MemberRegistration} bean that works when the transaction type of the persistence unit is set to
 * RESOURCE_LOCAL
 *
 * @author Davide D'Alto
 */
@Stateful
public class MemberRegistrationWithResourceLocal implements MemberRegistration {

	@PersistenceUnit
	private EntityManagerFactory factory;

	private EntityManager em;

	private Member newMember;

	@Override
	public Member getNewMember() {
		return newMember;
	}

	@Override
	public void register() {
		beginTransaction();
		RegistrationExecutor.register( em, newMember );
		commit();
		initNewMember();
	}

	private void commit() {
		em.getTransaction().commit();
		em.clear();
	}

	private void beginTransaction() {
		em.getTransaction().begin();
	}

	@Override
	public Member find(Long id) {
		beginTransaction();
		Member member = RegistrationExecutor.find( em, id );
		commit();
		return member;
	}

	@Override
	public Member findWithNativeQuery(String nativeQuery) {
		beginTransaction();
		Member member = RegistrationExecutor.findWithNativeQuery( em, nativeQuery );
		commit();
		return member;
	}

	@Override
	public Member findWithQuery(Long id) {
		beginTransaction();
		Member member = RegistrationExecutor.findWithQuery( em, id );
		commit();
		return member;
	}

	@Override
	public Member findWithEmail(String email) {
		beginTransaction();
		Member uniqueResult = RegistrationExecutor.findWithEmail( em, email );
		commit();
		return uniqueResult;
	}

	@PostConstruct
	public void initNewMember() {
		newMember = new Member();
		em = factory.createEntityManager();
	}

	@Override
	public void close() {
		em.close();
	}
}
