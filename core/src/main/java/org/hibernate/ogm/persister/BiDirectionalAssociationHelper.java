/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister;

import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.persister.impl.CollectionPhysicalModel;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;


/**
 * @author Gunnar Morling
 *
 */
public class BiDirectionalAssociationHelper {

	private final SessionFactoryImplementor factory;

	public BiDirectionalAssociationHelper(SessionFactoryImplementor factory) {
		this.factory = factory;
	}
	public AssociationKeyMetadata getInverseAssociationKeyMetadata(OgmEntityPersister mainSidePersister, int propertyIndex) {
		OgmEntityPersister associatedPersister = (OgmEntityPersister) ( (EntityType) mainSidePersister.getPropertyTypes()[propertyIndex] )
				.getAssociatedJoinable( factory );

		//code logic is slightly duplicated but the input and context is different, hence this choice
		Type[] propertyTypes = associatedPersister.getPropertyTypes();

		for ( int index = 0 ; index <  propertyTypes.length ; index++ ) {
			Type type = propertyTypes[index];

			//we try and restrict type search as much as possible
			//we look for associations that also are collections
			if ( type.isAssociationType() && type.isCollectionType() ) {
				return isCollectionMatching( (CollectionType) type, mainSidePersister );
			}
			//we look for associations that are to-one
			else if ( type.isAssociationType() && ! type.isCollectionType() ) { //isCollectionType redundant but kept for readability
				return isToOneMatching( associatedPersister, index, type, mainSidePersister.getPropertyTypes()[propertyIndex] );
			}
		}

		return null;
	}

	public AssociationKeyMetadata getInverseAssociationKeyMetadata(OgmCollectionPersister mainSidePersister) {
		Loadable elementPersister = (Loadable) mainSidePersister.getElementPersister();
		Type[] propertyTypes = elementPersister.getPropertyTypes();

		for ( int index = 0 ; index <  propertyTypes.length ; index++ ) {
			Type type = propertyTypes[index];
			//we try and restrict type search as much as possible
			if ( type.isAssociationType() ) {
				//if the main side collection is not a one-to-many, the reverse side should be a collection
				if ( ! mainSidePersister.isOneToMany() && type.isCollectionType() ) {
					return isCollectionMatching( (CollectionType) type, mainSidePersister );
				}
			}
		}
		return null;
	}

	private AssociationKeyMetadata isToOneMatching(OgmEntityPersister elementPersister, int index, Type type, Type typeOnMainSide) {
		if ( ( (EntityType) type ).isOneToOne() ) {
			// If that's a OneToOne check the associated property name and see if it matches where we come from
			// we need to do that as OneToOne don't define columns
			OneToOneType oneToOneType = (OneToOneType) type;
			String associatedProperty = oneToOneType.getRHSUniqueKeyPropertyName();
			if ( associatedProperty != null ) {
				OgmEntityPersister mainSidePersister = (OgmEntityPersister) oneToOneType.getAssociatedJoinable( factory );
				try {
					int propertyIndex = mainSidePersister.getPropertyIndex( associatedProperty );
					if ( mainSidePersister.getPropertyTypes()[propertyIndex] == typeOnMainSide ) {
						return elementPersister.getAssociationKeyMetadata( elementPersister.getPropertyNames()[index] );
					}
				}
				catch ( HibernateException e ) {
					//not the right property
					//probably should not happen
				}
			}
		}

		return null;
		//otherwise we do a key column comparison to see if it matches
//		return Arrays.equals( null, elementPersister.getPropertyColumnNames( index ) );
	}

	private AssociationKeyMetadata isCollectionMatching(CollectionType type, Joinable mainSidePersister) {
		// Find the reverse side collection and check if the table name and key columns are matching
		// what we have on the main side
		String collectionRole = type.getRole();
		CollectionPhysicalModel reverseCollectionPersister = (CollectionPhysicalModel) factory.getCollectionPersister( collectionRole );
		boolean isSameTable = mainSidePersister.getTableName().equals( reverseCollectionPersister.getTableName() );
		if ( isSameTable && Arrays.equals( mainSidePersister.getKeyColumnNames(), reverseCollectionPersister.getElementColumnNames() ) ) {
			return ((OgmCollectionPersister) reverseCollectionPersister ).getAssociationKeyMetadata();
		}

		return null;
	}
}
