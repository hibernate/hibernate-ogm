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
