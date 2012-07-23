/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.test.mongodb.loading;

import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.datastore.mongodb.AssociationStorage;
import org.hibernate.ogm.datastore.mongodb.Environment;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.mongodb.MongoDBAssociationSnapshot;
import org.hibernate.ogm.dialect.mongodb.MongoDBDialect;
import org.hibernate.ogm.grid.AssociationKey;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class LoadSelectedColumnsGlobalTest extends LoadSelectedColumnsTest {
	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty(
				Environment.MONGODB_ASSOCIATIONS_STORE,
				AssociationStorage.GLOBAL_COLLECTION.toString().toLowerCase()
		);
	}

	/**
	 * To be sure the datastoreProvider retrieves only the columns we want,
	 * an extra column is manually added to the association document
	 */
	@Override
	protected void addExtraColumn(){
		MongoDBDatastoreProvider provider = (MongoDBDatastoreProvider) super.getService( DatastoreProvider.class );
		DB database = provider.getDatabase();
		DBCollection collection = database.getCollection( "Associations" );

		final BasicDBObject idObject = new BasicDBObject( 2 );
		idObject.append( "Project_id", "projectID" );
		idObject.append( "table", "Project_Module" );

		BasicDBObject query = new BasicDBObject( 1 );
		query.put( "_id", idObject );

		BasicDBObject updater = new BasicDBObject( 1 );
		updater.put( "$push", new BasicDBObject( "extraColumn", 1 ) );
		collection.update( query, updater );
	}

	@Override
	protected void checkLoading() {
		GridDialect gridDialect = this.getGridDialect();
		AssociationKey associationKey = new AssociationKey(
				"Project_Module",
				new String[] { "Project_id", "table" },
				new Object[] { "projectID", "Project_Module" }
		);
		associationKey.setRowKeyColumnNames( new String[]{"Project_id", "module_id"} );

		AssociationContext associationContext = new AssociationContext();
		final Association association = gridDialect.getAssociation( associationKey, associationContext );
		final MongoDBAssociationSnapshot associationSnapshot = (MongoDBAssociationSnapshot) association.getSnapshot();
		final DBObject assocObject = associationSnapshot.getAssoc();

		/*
		* The only column (except _id) that needs to be retrieved is "rows"
		* So we should have 2 columns
		*/
		final Set<?> retrievedColumns = assocObject.keySet();
		assertThat( retrievedColumns ).hasSize( 2 ).containsOnly( MongoDBDialect.ID_FIELDNAME, MongoDBDialect.ROWS_FIELDNAME );
	}
}
