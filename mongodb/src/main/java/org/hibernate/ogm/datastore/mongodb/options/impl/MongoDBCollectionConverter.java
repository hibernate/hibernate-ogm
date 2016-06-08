/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.impl;

import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.OptionValuePair;


/**
 * Converts the {@code @MongoDBCollection} annotation into a MongoDBCollection.
 *
 * @author Guillaume Smet
 */
public class MongoDBCollectionConverter implements AnnotationConverter<org.hibernate.ogm.datastore.mongodb.options.MongoDBCollection> {

	@Override
	public OptionValuePair<?> convert(org.hibernate.ogm.datastore.mongodb.options.MongoDBCollection annotation) {
		return OptionValuePair.getInstance( new MongoDBCollectionOption(), new MongoDBCollection( annotation ) );
	}

}
