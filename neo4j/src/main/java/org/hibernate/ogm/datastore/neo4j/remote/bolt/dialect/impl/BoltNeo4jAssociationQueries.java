/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl;

import java.util.Arrays;
import java.util.Map;

import org.hibernate.ogm.datastore.neo4j.dialect.impl.BaseNeo4jAssociationQueries;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.util.impl.ArrayHelper;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.types.Relationship;

/**
 * @author Davide D'Alto
 */
public class BoltNeo4jAssociationQueries extends BaseNeo4jAssociationQueries {

	public BoltNeo4jAssociationQueries(EntityKeyMetadata ownerEntityKeyMetadata, AssociationKeyMetadata associationKeyMetadata) {
		super( ownerEntityKeyMetadata, associationKeyMetadata );
	}

	public void removeAssociation(Transaction tx, AssociationKey associationKey) {
		tx.run( removeAssociationQuery, params( associationKey.getEntityKey().getColumnValues() ) );
	}

	public Relationship findRelationship(Transaction tx, AssociationKey associationKey, RowKey rowKey) {
		Object[] relationshipValues = relationshipValues( associationKey, rowKey );
		Object[] queryValues = ArrayHelper.concat( associationKey.getEntityKey().getColumnValues(), relationshipValues );
		StatementResult result = tx.run( findRelationshipQuery, params( queryValues ) );
		return relationship( result );
	}

	private Relationship relationship(StatementResult result) {
		if ( result.hasNext() ) {
			return result.next().get( 0 ).asRelationship();
		}
		return null;
	}

	public Relationship createRelationshipForEmbeddedAssociation(Transaction tx, AssociationKey associationKey, EntityKey embeddedKey,
			Object[] relationshipProperties) {
		String query = initCreateEmbeddedAssociationQuery( associationKey, embeddedKey );
		Object[] queryValues = createRelationshipForEmbeddedQueryValues( associationKey, embeddedKey, relationshipProperties );
		StatementResult statementResult = tx.run( query, params( queryValues ) );
		return relationship( statementResult );
	}

	public Relationship createRelationship(Transaction tx, Object[] ownerKeyValues, Object[] targetKeyValues, Object[] relationshipProperties) {
		Object[] concat = ArrayHelper.concat( Arrays.asList( ownerKeyValues, targetKeyValues, relationshipProperties ) );
		Map<String, Object> params = params( concat );
		StatementResult statementResult = tx.run( createRelationshipQuery, params );
		return relationship( statementResult );
	}

	public void removeAssociationRow(Transaction tx, AssociationKey associationKey, RowKey rowKey) {
		Object[] relationshipValues = relationshipValues( associationKey, rowKey );
		Object[] queryValues = ArrayHelper.concat( associationKey.getEntityKey().getColumnValues(), relationshipValues );
		tx.run( removeAssociationRowQuery, params( queryValues ) );
	}
}
