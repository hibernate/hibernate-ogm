package org.hibernate.ogm.test.mongodb.associations;

import org.hibernate.ogm.datastore.mongodb.AssociationStorage;
import org.hibernate.ogm.datastore.mongodb.Environment;
import org.hibernate.ogm.test.id.CompositeIdTest;
import org.hibernate.ogm.test.utils.jpa.GetterPersistenceUnitInfo;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class CompositeIdInEmbeddedTest extends CompositeIdTest {
	@Override
	protected void refineInfo(GetterPersistenceUnitInfo info) {
		super.refineInfo( info );
		info.getProperties().setProperty(
				Environment.MONGODB_ASSOCIATIONS_STORE,
				AssociationStorage.IN_ENTITY.toString().toLowerCase()
		);
	}
}
