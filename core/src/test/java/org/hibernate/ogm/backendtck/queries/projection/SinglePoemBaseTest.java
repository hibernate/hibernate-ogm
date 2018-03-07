/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries.projection;

import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * @author Fabio Massimo Ercoli
 */
public abstract class SinglePoemBaseTest extends OgmTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	protected final Poem originalPoem = new Poem( 1L, "Portia", "Oscar Wilde", 1881 );

	@Before
	public void init() {
		inTransaction( session -> session.persist( originalPoem ) );
	}

	@After
	public void tearDown() {
		inTransaction( session -> {
			Object entity = session.get( Poem.class, originalPoem.getId() );
			if ( entity != null ) {
				session.delete( entity );
			}
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Poem.class };
	}
}
