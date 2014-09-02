/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss.controller;

import javax.persistence.EntityManager;

import org.apache.lucene.search.Query;
import org.hibernate.ogm.test.integration.jboss.model.Member;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

/**
 * Methods to execute the registration and search of {@link Member}s
 *
 * @author Davide D'Alto
 */
public class RegistrationExecutor {

	private RegistrationExecutor() {
	}

	public static void register(EntityManager em, Member newMember) {
		em.persist( newMember );
	}

	public static Member find(EntityManager em, Long id) {
		return em.find( Member.class, id );
	}

	public static Member findWithNativeQuery(EntityManager em, String nativeQuery) {
		return (Member) em.createNativeQuery( nativeQuery, Member.class).getSingleResult();
	}

	public static Member findWithQuery(EntityManager em, Long id) {
		return em.createQuery( "FROM Member WHERE id = :id", Member.class)
				.setParameter( "id", id )
				.getSingleResult();
	}

	public static Member findWithEmail(EntityManager em, String email) {
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		QueryBuilder b = ftem.getSearchFactory().buildQueryBuilder().forEntity( Member.class ).get();
		Query lq = b.keyword().wildcard().onField( "email" ).matching( email ).createQuery();
		Object uniqueResult = ftem.createFullTextQuery( lq ).getSingleResult();
		return (Member) uniqueResult;
	}
}
