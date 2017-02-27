/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.gettingstarted;

import org.hibernate.ogm.examples.gettingstarted.DogBreedRunner;
import org.junit.Test;

public class GettingstartedGuideBootsTest {

	@Test
	public void bootsWithoutExceptions() {
		DogBreedRunner.main( new String[0] );
	}

}
