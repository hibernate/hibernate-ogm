/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id.embeddable;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Test;

/**
 * Tests for embeddable id type containing a single column.
 *
 * @author Gunnar Morling
 */
public class EmbeddableIdTest extends OgmTestCase {

	@Test
	@TestForIssue(jiraKey = "OGM-917")
	public void canRetrieveListOfEntityWithSingleColumnEmbeddableId() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// given
		session.persist( new SingleBoardComputer( new SingleBoardComputerPk( "sbc-1" ), "Raspberry Pi" ) );
		session.persist( new SingleBoardComputer( new SingleBoardComputerPk( "sbc-2" ), "BeagleBone" ) );

		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();

		// when
		@SuppressWarnings("unchecked")
		List<SingleBoardComputer> computers = session.createQuery( "From SingleBoardComputer" ).list();

		// then
		assertThat( computers ).onProperty( "name" ).containsOnly( "Raspberry Pi", "BeagleBone" );

		for ( SingleBoardComputer sbc : computers ) {
			session.delete( sbc );
		}

		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SingleBoardComputer.class };
	}
}
