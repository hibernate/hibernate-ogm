/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.perftest.mongodb.nativeapi;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Random;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

/**
 * Base class for MongoDB native API benchmarks.
 *
 * @author Gunnar Morling
 */
public class NativeApiBenchmarkBase {

	private static Properties properties = new Properties();

	static {
		try {
			properties.load( NativeApiBenchmarkBase.class.getClassLoader().getResourceAsStream( "native-settings.properties" ) );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	@State(Scope.Benchmark)
	public static class ClientHolder {

		MongoClient mongo;
		DB db;
		Random rand;

		@Setup
		public void setupDatastore() throws Exception {
			MongoClient mongo = getMongoClient();
			db = mongo.getDB( properties.getProperty( "database" ) );
			db.dropDatabase();
			rand = new Random();
		}
	}

	protected static MongoClient getMongoClient() throws UnknownHostException {
		ServerAddress serverAddress = new ServerAddress( properties.getProperty( "host" ), 27017 );

		MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder();

		optionsBuilder.connectTimeout( 1000 );
		optionsBuilder.writeConcern( WriteConcern.ACKNOWLEDGED );
		optionsBuilder.readPreference( ReadPreference.primary() );

		MongoClientOptions clientOptions = optionsBuilder.build();

		MongoClient mongo = new MongoClient( serverAddress, clientOptions );

		return mongo;
	}
}
