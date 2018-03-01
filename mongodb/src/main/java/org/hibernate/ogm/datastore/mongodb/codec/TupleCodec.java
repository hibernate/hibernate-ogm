/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.codec;

import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot;
import org.hibernate.ogm.model.spi.Tuple;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class TupleCodec implements Codec<Tuple> {
	@Override
	public Tuple decode(BsonReader reader, DecoderContext decoderContext) {
		DocumentCodec documentCodec = new DocumentCodec(  );

		Document document = documentCodec.decode( reader,decoderContext );
		MongoDBTupleSnapshot snapshot = new MongoDBTupleSnapshot( document,null );
		return new Tuple( snapshot, Tuple.SnapshotType.UNKNOWN );
	}

	@Override
	public void encode(BsonWriter writer, Tuple value, EncoderContext encoderContext) {
		MongoDBTupleSnapshot snapshot = (MongoDBTupleSnapshot) value.getSnapshot();
		Document document = snapshot.getDbObject();
		DocumentCodec documentCodec = new DocumentCodec(  );
		documentCodec.encode( writer,document,encoderContext );
	}

	@Override
	public Class<Tuple> getEncoderClass() {
		return Tuple.class;
	}
}
