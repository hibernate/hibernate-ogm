/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.codec;

import org.hibernate.ogm.model.spi.Tuple;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class TupleCodecProvider implements CodecProvider {
	@Override
	public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
		if ( clazz == Tuple.class ) {
			return (Codec<T>) new TupleCodec();
		}

		// CodecProvider returns null if it's not a provider for the requresed Class
		return null;
	}
}
