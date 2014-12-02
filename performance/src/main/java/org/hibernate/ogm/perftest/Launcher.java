/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.perftest;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Launches all the JMH benchmarks within this project. Be sure to build the project before to let the JMH annotation
 * processor generate all the required infrastructure.
 *
 * @author Gunnar Morling
 */
public class Launcher {

	public static void main(String... args) throws Exception {
		Options opts = new OptionsBuilder()
			.include( ".*" )
			.warmupIterations( 20 )
			.measurementIterations( 20 )
			.jvmArgs( "-server" )
			.jvmArgsAppend( "-XX:+UnlockCommercialFeatures", "-XX:+FlightRecorder" )
//			.jvmArgs( "-server", "-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=y" )
			.forks( 1 )
			.build();

		new Runner(opts).run();
	}
}
