/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.persister;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.backendtck.associations.collection.manytomany.AccountOwner;
import org.hibernate.ogm.backendtck.associations.collection.manytomany.BankAccount;
import org.hibernate.ogm.backendtck.associations.collection.types.Address;
import org.hibernate.ogm.backendtck.associations.collection.types.PhoneNumber;
import org.hibernate.ogm.backendtck.associations.collection.types.Race;
import org.hibernate.ogm.backendtck.associations.collection.types.Runner;
import org.hibernate.ogm.backendtck.associations.collection.types.User;
import org.hibernate.ogm.backendtck.associations.manytoone.SalesForce;
import org.hibernate.ogm.backendtck.associations.manytoone.SalesGuy;
import org.hibernate.ogm.backendtck.associations.onetoone.Husband;
import org.hibernate.ogm.backendtck.associations.onetoone.Wife;
import org.hibernate.ogm.backendtck.embeddable.MultiAddressAccount;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationType;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Tests for obtaining association key metadata.
 *
 * @author Gunnar Morling
 */
public class AssociationKeyMetadataTest extends OgmTestCase {

	@Test
	public void testAssociationType() {
		AssociationKeyMetadata akm = getCollectionPersister( BankAccount.class.getName() + ".owners" ).getAssociationKeyMetadata();
		assertThat( akm.getAssociationType() ).isEqualTo( AssociationType.SET );

		akm = getCollectionPersister( MultiAddressAccount.class.getName() + ".addresses" ).getAssociationKeyMetadata();
		assertThat( akm.getAssociationType() ).isEqualTo( AssociationType.BAG );

		akm = getCollectionPersister( Race.class.getName() + ".runnersByArrival" ).getAssociationKeyMetadata();
		assertThat( akm.getAssociationType() ).isEqualTo( AssociationType.LIST );

		akm = getCollectionPersister( User.class.getName() + ".addresses" ).getAssociationKeyMetadata();
		assertThat( akm.getAssociationType() ).isEqualTo( AssociationType.MAP );

		akm = getEntityPersister( Husband.class.getName() ).getInverseOneToOneAssociationKeyMetadata( "wife" );
		assertThat( akm.getAssociationType() ).isEqualTo( AssociationType.ONE_TO_ONE );
	}

	@Test
	public void testEntityKeyMetadata() {
		AssociationKeyMetadata akm = getCollectionPersister( BankAccount.class.getName() + ".owners" ).getAssociationKeyMetadata();
		assertThat( akm.getEntityKeyMetadata().getTable() ).isEqualTo( "BankAccount" );

		akm = getCollectionPersister( AccountOwner.class.getName() + ".bankAccounts" ).getAssociationKeyMetadata();
		assertThat( akm.getEntityKeyMetadata().getTable() ).isEqualTo( "AccountOwner" );

		akm = getCollectionPersister( SalesForce.class.getName() + ".salesGuys" ).getAssociationKeyMetadata();
		assertThat( akm.getEntityKeyMetadata().getTable() ).isEqualTo( "SalesForce" );

		akm = getEntityPersister( Husband.class.getName() ).getInverseOneToOneAssociationKeyMetadata( "wife" );
		assertThat( akm.getEntityKeyMetadata().getTable() ).isEqualTo( "Wife" );
	}

	private OgmEntityPersister getEntityPersister(String entityName) {
		return (OgmEntityPersister) ( getSessionFactory() ).getMetamodel().entityPersister( entityName );
	}

	private OgmCollectionPersister getCollectionPersister(String role) {
		return (OgmCollectionPersister) ( getSessionFactory() ).getMetamodel().collectionPersister( role );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BankAccount.class, AccountOwner.class, Husband.class, Wife.class, MultiAddressAccount.class, Address.class, Race.class,
				Runner.class, User.class, PhoneNumber.class, SalesForce.class, SalesGuy.class };
	}
}
