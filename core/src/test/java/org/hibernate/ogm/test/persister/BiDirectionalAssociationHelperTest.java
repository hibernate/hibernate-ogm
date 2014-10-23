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
import org.hibernate.ogm.backendtck.associations.collection.unidirectional.Cloud;
import org.hibernate.ogm.backendtck.associations.collection.unidirectional.SnowFlake;
import org.hibernate.ogm.backendtck.associations.manytoone.SalesForce;
import org.hibernate.ogm.backendtck.associations.manytoone.SalesGuy;
import org.hibernate.ogm.backendtck.associations.onetoone.Husband;
import org.hibernate.ogm.backendtck.associations.onetoone.Wife;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.persister.impl.BiDirectionalAssociationHelper;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Test for {@link BiDirectionalAssociationHelper}.
 *
 * @author Gunnar Morling
 */
public class BiDirectionalAssociationHelperTest extends OgmTestCase {

	@Test
	public void inverseMetadataForUnidirectionalOneToManyShouldBeNull() {
		OgmCollectionPersister persister = (OgmCollectionPersister) sessions.getCollectionPersister( Cloud.class.getName() + ".producedSnowFlakes" );
		OgmCollectionPersister inversePersister = BiDirectionalAssociationHelper.getInverseCollectionPersister( persister );
		assertThat( inversePersister ).isNull();
	}

	@Test
	public void canObtainInverseMetadataForBiDirectionalManyToOne() {
		// obtain inverse collection from the main side
		OgmEntityPersister entityPersister = (OgmEntityPersister) sessions.getEntityPersister( SalesGuy.class.getName() );
		AssociationKeyMetadata inverseAssociationKeyMetadata = BiDirectionalAssociationHelper.getInverseAssociationKeyMetadata( entityPersister, entityPersister.getPropertyIndex( "salesForce" ) );
		assertThat( inverseAssociationKeyMetadata ).isNotNull();
		assertThat( inverseAssociationKeyMetadata.getTable() ).isEqualTo( "SalesGuy" );
		assertThat( inverseAssociationKeyMetadata.getColumnNames() ).isEqualTo( new String[]{ "salesForce_id" } );
		assertThat( inverseAssociationKeyMetadata.getRowKeyColumnNames() ).isEqualTo( new String[]{ "salesForce_id", "id" } );

		// no persister on the main side
		OgmCollectionPersister collectionPersister = (OgmCollectionPersister) sessions.getCollectionPersister( SalesForce.class.getName() + ".salesGuys" );
		OgmCollectionPersister mainSidePersister = BiDirectionalAssociationHelper.getInverseCollectionPersister( collectionPersister );
		assertThat( mainSidePersister ).isNull();
	}

	@Test
	public void canObtainInverseMetadataForBiDirectionalManyToMany() {
		// obtain inverse collection from the main side
		OgmEntityPersister entityPersister = (OgmEntityPersister) sessions.getEntityPersister( AccountOwner.class.getName() );
		AssociationKeyMetadata inverseAssociationKeyMetadata = BiDirectionalAssociationHelper.getInverseAssociationKeyMetadata( entityPersister, entityPersister.getPropertyIndex( "bankAccounts" ) );
		assertThat( inverseAssociationKeyMetadata ).isNotNull();
		assertThat( inverseAssociationKeyMetadata.getTable() ).isEqualTo( "AccountOwner_BankAccount" );
		assertThat( inverseAssociationKeyMetadata.getColumnNames() ).isEqualTo( new String[]{ "bankAccounts_id" } );
		assertThat( inverseAssociationKeyMetadata.getRowKeyColumnNames() ).isEqualTo( new String[]{ "bankAccounts_id", "owners_id" } );

		// obtain main side collection from the inverse side
		entityPersister = (OgmEntityPersister) sessions.getEntityPersister( BankAccount.class.getName() );
		AssociationKeyMetadata mainSideAssociationKeyMetadata = BiDirectionalAssociationHelper.getInverseAssociationKeyMetadata( entityPersister, entityPersister.getPropertyIndex( "owners" ) );
		assertThat( mainSideAssociationKeyMetadata ).isNotNull();
		assertThat( mainSideAssociationKeyMetadata.getTable() ).isEqualTo( "AccountOwner_BankAccount" );
		assertThat( mainSideAssociationKeyMetadata.getColumnNames() ).isEqualTo( new String[]{ "owners_id" } );
		assertThat( mainSideAssociationKeyMetadata.getRowKeyColumnNames() ).isEqualTo( new String[]{ "owners_id", "bankAccounts_id" } );
	}

	@Test
	public void canHandleSeveralAssociationsOnInverseSide() {
		// obtain inverse collection from the main side
		OgmEntityPersister entityPersister = (OgmEntityPersister) sessions.getEntityPersister( Pancake.class.getName() );
		AssociationKeyMetadata inverseAssociationKeyMetadata = BiDirectionalAssociationHelper.getInverseAssociationKeyMetadata( entityPersister, entityPersister.getPropertyIndex( "eater" ) );
		assertThat( inverseAssociationKeyMetadata ).isNotNull();
		assertThat( inverseAssociationKeyMetadata.getTable() ).isEqualTo( "Pancake" );
		assertThat( inverseAssociationKeyMetadata.getColumnNames() ).isEqualTo( new String[]{ "eater_id" } );
		assertThat( inverseAssociationKeyMetadata.getRowKeyColumnNames() ).isEqualTo( new String[]{ "eater_id", "id" } );

		// obtain inverse collection from the main side, different entity
		entityPersister = (OgmEntityPersister) sessions.getEntityPersister( Muffin.class.getName() );
		inverseAssociationKeyMetadata = BiDirectionalAssociationHelper.getInverseAssociationKeyMetadata( entityPersister, entityPersister.getPropertyIndex( "eater" ) );
		assertThat( inverseAssociationKeyMetadata ).isNotNull();
		assertThat( inverseAssociationKeyMetadata.getTable() ).isEqualTo( "Muffin" );
		assertThat( inverseAssociationKeyMetadata.getColumnNames() ).isEqualTo( new String[]{ "eater_id" } );
		assertThat( inverseAssociationKeyMetadata.getRowKeyColumnNames() ).isEqualTo( new String[]{ "eater_id", "id" } );
	}

	@Test
	public void canObtainInverseMetadataForBiDirectionalOneToOne() {
		// obtain inverse collection from the main side
		OgmEntityPersister entityPersister = (OgmEntityPersister) sessions.getEntityPersister( Husband.class.getName() );
		AssociationKeyMetadata inverseAssociationKeyMetadata = BiDirectionalAssociationHelper.getInverseAssociationKeyMetadata( entityPersister, entityPersister.getPropertyIndex( "wife" ) );
		assertThat( inverseAssociationKeyMetadata ).isNotNull();
		assertThat( inverseAssociationKeyMetadata.getTable() ).isEqualTo( "Husband" );
		assertThat( inverseAssociationKeyMetadata.getColumnNames() ).isEqualTo( new String[]{ "wife" } );
		assertThat( inverseAssociationKeyMetadata.getRowKeyColumnNames() ).isEqualTo( new String[]{ "id", "wife" } );
	}

	@Test
	public void canObtainMainSidePropertyNameForOneToMany() {
		OgmCollectionPersister persister = (OgmCollectionPersister) sessions.getCollectionPersister( SalesForce.class.getName() + ".salesGuys" );
		String mainSidePropertyName = BiDirectionalAssociationHelper.getMainSidePropertyName( persister );
		assertThat( mainSidePropertyName ).isEqualTo( "salesForce" );
	}

	@Test
	public void canObtainMainSidePropertyNameForManyToMany() {
		// ask from main side
		OgmCollectionPersister persister = (OgmCollectionPersister) sessions.getCollectionPersister( AccountOwner.class.getName() + ".bankAccounts" );
		String mainSidePropertyName = BiDirectionalAssociationHelper.getMainSidePropertyName( persister );
		assertThat( mainSidePropertyName ).isEqualTo( "bankAccounts" );

		// ask from inverse side
		persister = (OgmCollectionPersister) sessions.getCollectionPersister( BankAccount.class.getName() + ".owners" );
		mainSidePropertyName = BiDirectionalAssociationHelper.getMainSidePropertyName( persister );
		assertThat( mainSidePropertyName ).isEqualTo( "bankAccounts" );
	}

	@Test
	public void canObtainMainSidePropertyNameWithSeveralAssociationsOnInverseSide() {
		OgmCollectionPersister persister = (OgmCollectionPersister) sessions.getCollectionPersister( Eater.class.getName() + ".muffins" );
		String mainSidePropertyName = BiDirectionalAssociationHelper.getMainSidePropertyName( persister );
		assertThat( mainSidePropertyName ).isEqualTo( "eater" );

		persister = (OgmCollectionPersister) sessions.getCollectionPersister( Eater.class.getName() + ".muffinsEatenAsStandin" );
		mainSidePropertyName = BiDirectionalAssociationHelper.getMainSidePropertyName( persister );
		assertThat( mainSidePropertyName ).isEqualTo( "standinEater" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Cloud.class, SnowFlake.class, SalesForce.class, SalesGuy.class, BankAccount.class, AccountOwner.class, Pancake.class,
				Muffin.class, Eater.class, Wife.class, Husband.class };
	}
}
