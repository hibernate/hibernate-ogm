/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.associations.ordercolumn;

import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDbObject;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Test for the mapping of bi-directional associations with order columns.
 *
 * @author Gunnar Morling
 */
public class OrderColumnMappingTest extends OgmTestCase {

	@Test
	public void testOrderedBiDirectionalManyToOneMapping() throws Exception {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// Given
		TvShow bakingBread = new TvShow( "tvshow-1", "Baking Bread" );

		Episode preparingTheDough = new Episode( "episode-1", "Preparing the Dough", bakingBread );
		bakingBread.getEpisodes().add( preparingTheDough );

		Episode heatingTheOven = new Episode( "episode-2", "Heating the Oven", bakingBread );
		bakingBread.getEpisodes().add( heatingTheOven );

		Episode bakingTheBread = new Episode( "episode-3", "Baking the Bread", bakingBread );
		bakingBread.getEpisodes().add( bakingTheBread );

		// When
		session.persist( bakingBread );
		tx.commit();
		session.clear();

		tx = session.beginTransaction();

		// Then
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"TvShow",
				// query
				"{ '_id' : 'tvshow-1' }",
				// expected
				"{" +
					"'_id' : 'tvshow-1', " +
					"'episodes' : [ " +
						"{ 'idx' : 2, 'id' : 'episode-3'} ," +
						"{ 'idx' : 1, 'id' : 'episode-2'} ," +
						"{ 'idx' : 0, 'id' : 'episode-1'}" +
					"]," +
					"'name' : 'Baking Bread'" +
				"}"
		);

		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Episode",
				// query
				"{ '_id' : 'episode-1' }",
				// expected
				"{ " +
					"'_id' : 'episode-1', " +
					"'name' : 'Preparing the Dough'," +
					"'tv_show_id' : 'tvshow-1'" +
				"}"
		);

		// Clean-Up
		bakingBread = (TvShow) session.get( TvShow.class, "tvshow-1" );
		session.delete( bakingBread );

		tx.commit();
		session.close();

		checkCleanCache();
	}

	@Test
	public void testOrderedBiDirectionalManyToManyMapping() throws Exception {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// Given
		Episode preparingTheDough = new Episode( "episode-1", "Preparing the Dough", null );
		Episode heatingTheOven = new Episode( "episode-2", "Heating the Oven", null );

		Writer john = new Writer( "writer-1", "John" );
		Writer pricilla = new Writer( "writer-2", "Pricilla" );
		Writer ernst = new Writer( "writer-3", "Ernst" );

		preparingTheDough.getAuthors().add( ernst );
		ernst.getEpisodes().add( preparingTheDough );

		preparingTheDough.getAuthors().add( john );
		john.getEpisodes().add( preparingTheDough );

		heatingTheOven.getAuthors().add( john );
		john.getEpisodes().add( heatingTheOven );

		// When
		session.persist( preparingTheDough );
		session.persist( heatingTheOven );
		session.persist( john );
		session.persist( pricilla );
		session.persist( ernst );

		tx.commit();
		session.clear();

		tx = session.beginTransaction();

		// Then
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Writer",
				// query
				"{ '_id' : 'writer-1' }",
				// expected
				"{" +
					"'_id' : 'writer-1', " +
					"'episodes' : ['episode-1' , 'episode-2']," +
					"'name' : 'John'" +
				"}"
		);

		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Writer",
				// query
				"{ '_id' : 'writer-3' }",
				// expected
				"{" +
					"'_id' : 'writer-3', " +
					"'episodes' : ['episode-1']," +
					"'name' : 'Ernst'" +
				"}"
		);

		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Episode",
				// query
				"{ '_id' : 'episode-1' }",
				// expected
				"{ " +
					"'_id' : 'episode-1', " +
					"'authors' : [" +
						"{ 'authorOrder' : 1, 'authorId' : 'writer-1' }," +
						"{ 'authorOrder' : 0, 'authorId' : 'writer-3' }" +
					"]," +
					"'name' : 'Preparing the Dough'" +
				"}"
		);

		// Clean-Up
		Writer writerToDelete = (Writer) session.get( Writer.class, "writer-1" );
		session.delete( writerToDelete );

		writerToDelete = (Writer) session.get( Writer.class, "writer-2" );
		session.delete( writerToDelete );

		writerToDelete = (Writer) session.get( Writer.class, "writer-3" );
		session.delete( writerToDelete );

		Episode episodeToDelete = (Episode) session.get( Episode.class, "episode-1" );
		session.delete( episodeToDelete );

		episodeToDelete = (Episode) session.get( Episode.class, "episode-2" );
		session.delete( episodeToDelete );

		tx.commit();
		session.close();

		checkCleanCache();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TvShow.class, Episode.class, Writer.class };
	}
}
