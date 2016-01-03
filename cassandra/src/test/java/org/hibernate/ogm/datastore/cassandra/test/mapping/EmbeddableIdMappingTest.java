/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.test.mapping;

import static org.hibernate.ogm.datastore.cassandra.utils.CassandraTestHelper.rowAssertion;

import java.util.Collections;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.id.Label;
import org.hibernate.ogm.backendtck.id.News;
import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.ogm.backendtck.id.embeddable.SingleBoardComputer;
import org.hibernate.ogm.backendtck.id.embeddable.SingleBoardComputerPk;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the mappings of embeddable ids into Cassandra.
 *
 * @author Nicola Ferraro
 */
public class EmbeddableIdMappingTest extends OgmTestCase {

	@Before
	public void setUpTestData() {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		session.persist( new SingleBoardComputer( new SingleBoardComputerPk( "sbc-1" ), "Raspberry Pi" ) );

		session.persist(
				new News(
						new NewsID( "seven-projects", "Jerry" ),
						"Seven Raspberry Pi projects you would not believe were possible",
						Collections.<Label>emptyList() ) );

		transaction.commit();
		session.close();
	}

	@After
	public void removeTestData() {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		session.delete( session.load( SingleBoardComputer.class, new SingleBoardComputerPk( "sbc-1" ) ) );
		session.delete( session.load( News.class, new NewsID( "seven-projects", "Jerry" ) ) );
		transaction.commit();
		session.close();
	}

	@Test
	public void multiColumnEmbeddableIdMapping() {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		rowAssertion( session.getSessionFactory(), "News" )
				.keyColumn( "newsId.title", "seven-projects" )
				.keyColumn( "newsId.author", "Jerry" )
				.assertColumn( "content", "Seven Raspberry Pi projects you would not believe were possible" )
				.assertNoOtherColumnPresent()
				.execute();

		transaction.commit();
		session.close();
	}

	@Test
	public void singleColumnEmbeddableIdMapping() {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		rowAssertion( session.getSessionFactory(), "SingleBoardComputer" )
				.keyColumn( "id.id", "sbc-1" )
				.assertColumn( "name", "Raspberry Pi" )
				.assertNoOtherColumnPresent()
				.execute();

		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ SingleBoardComputer.class, News.class, Label.class };
	}
}
