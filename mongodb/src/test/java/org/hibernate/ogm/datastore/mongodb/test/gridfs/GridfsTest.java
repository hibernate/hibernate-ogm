/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.gridfs;

import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class GridfsTest extends OgmTestCase {

	@Test
	public void canUseGridFS() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		Apartment app1 = new Apartment();
		app1.setComment( "Nice ftat" );
		app1.setCountry( "Russia" );
		app1.setCity( "Sochi" );

		session.persist( app1 );

		Photo photo1 = new Photo();
		photo1.setCreated( Calendar.getInstance().getTime() );
		photo1.setImage( new byte[]{ 1, 2, 3 } );
		photo1.setApartment( app1 );
		session.persist( photo1 );

		app1.setPhotos( new LinkedList<>( Collections.singletonList( photo1 ) ) );
		session.update( app1 );
		tx.commit();
		session.clear();

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Apartment.class, Photo.class };
	}

}
