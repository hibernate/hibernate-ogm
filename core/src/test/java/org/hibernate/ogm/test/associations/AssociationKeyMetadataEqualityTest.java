/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.associations;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.backendtck.associations.collection.types.Address;
import org.hibernate.ogm.backendtck.associations.collection.types.PhoneNumber;
import org.hibernate.ogm.backendtck.associations.collection.types.User;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class AssociationKeyMetadataEqualityTest extends OgmTestCase {

	@Test
	@TestForIssue(jiraKey = "OGM-969")
	// ORM does not support mapping different associations of the same entity using the same table.
	// Each association must have a different table. For this reason we decided to update the tests and rename the
	// tables. I'll leave this here just in case someone decides to change the test.
	public void testDefaultAssociationKeyMetadataEquals() throws Exception {
		OgmCollectionPersister collection1 = (OgmCollectionPersister) getSessionFactory().getMetamodel().collectionPersister( User.class.getName() + ".phoneNumbersByPriority" );
		AssociationKeyMetadata byPriority = collection1.getAssociationKeyMetadata();

		OgmCollectionPersister collection2 = (OgmCollectionPersister) getSessionFactory().getMetamodel().collectionPersister( User.class.getName() + ".alternativePhoneNumbers" );
		AssociationKeyMetadata alternatvePhoneNumbers = collection2.getAssociationKeyMetadata();

		assertThat( byPriority ).as( "Missing required association for testing" ).isNotNull();
		assertThat( alternatvePhoneNumbers ).as( "Missing required association for testing" ).isNotNull();
		assertThat( byPriority ).isNotEqualTo( alternatvePhoneNumbers );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ User.class, PhoneNumber.class, Address.class };
	}
}
