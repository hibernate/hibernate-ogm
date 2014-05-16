/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss.controller;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import javax.enterprise.inject.Model;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import org.apache.lucene.search.Query;
import org.hibernate.ogm.test.integration.jboss.model.Member;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

@Stateful
@Model
public class MemberRegistration {

	@Inject
	private EntityManager em;

	private Member newMember;

	@Produces
	@Named
	public Member getNewMember() {
		return newMember;
	}

	public void register() throws Exception {
		em.persist( newMember );
		initNewMember();
	}

	public Member find(Long id) {
		return em.find( Member.class, id );
	}

	public Member findWithNativeQuery(String nativeQuery) {
		return (Member) em.createNativeQuery( nativeQuery, Member.class).getSingleResult();
	}

	public Member findWithQuery(Long id) {
		return em.createQuery( "FROM Member WHERE id = :id", Member.class)
				.setParameter( "id", id )
				.getSingleResult();
	}

	public Member findWithEmail(String email) {
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		QueryBuilder b = ftem.getSearchFactory().buildQueryBuilder().forEntity( Member.class ).get();
		Query lq = b.keyword().wildcard().onField( "email" ).matching( email ).createQuery();
		Object uniqueResult = ftem.createFullTextQuery( lq ).getSingleResult();
		return (Member) uniqueResult;
	}

	@PostConstruct
	public void initNewMember() {
		newMember = new Member();
	}

}
