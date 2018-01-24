/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.wildfly.testcase.controller;

import java.util.List;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.search.Query;
import org.hibernate.ogm.test.integration.wildfly.testcase.model.MagicCard;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

@Stateful
public class MagicCardsCollectionBean {

	@PersistenceContext
	public EntityManager em;

	public void storeCard(MagicCard card) {
		em.persist( card );
	}

	public MagicCard loadById(Long id) {
		return em.find( MagicCard.class, id );
	}

	public List<MagicCard> findByName(String name) {
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager( em );
		QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity( MagicCard.class ).get();

		Query query = queryBuilder.keyword().onField( "name" ).matching( name ).createQuery();
		return fullTextEntityManager.createFullTextQuery( query, MagicCard.class ).getResultList();
	}

}
