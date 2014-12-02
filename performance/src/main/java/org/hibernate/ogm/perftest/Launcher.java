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
 * Launches all the JMH benchmarks within this project.
 * <p>
 * In order to run the benchmarks, do the following:
 * <ul>
 * <li>Generate the JMH benchmark classes by running {@code mvn generate-sources -pl performance} from the root dir</li>
 * <li>Adapt the settings in {@code persistence.xml} and/or {@code native-settings.properties} as per your environment
 * (both under {@code src/main/resources)}
 * <li>(optional:) Adapt the includes below to run a sub-set of all benchmarks
 * </ul>
 * Refer to the <a href="http://openjdk.java.net/projects/code-tools/jmh/">JMH documentation</a> to learn more about the
 * Java Micro-benchmark Harness in general.
 *
 * @author Gunnar Morling
 */
public class Launcher {

	/**
	 * Property used to specify VM arguments to be passed to the benchmark runner, e.g. like so:
	 * <pre>
	 * {@code
	 * java Launcher -DbenchmarkVmArgs="-XX:+UnlockCommercialFeatures -XX:+FlightRecorder"
	 * java Launcher -DbenchmarkVmArgs="-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=y"
	 * }
	 * </pre>
	 */
	private static final String BENCHMARK_VM_ARGS_KEY = "benchmarkVmArgs";

	public static void main(String... args) throws Exception {
		String benchmarkArgsString = System.getProperty( BENCHMARK_VM_ARGS_KEY );
		String[] benchMarkArgs;

		if ( benchmarkArgsString != null ) {
			benchMarkArgs = benchmarkArgsString.split( "\\s+" );
		}
		else {
			benchMarkArgs = new String[0];
		}

		Options opts = new OptionsBuilder()
			.include( ".*" )
			.warmupIterations( 20 )
			.measurementIterations( 20 )
			.jvmArgs( "-server" )
			.jvmArgsAppend( benchMarkArgs )
			.forks( 1 )
			.build();

		new Runner(opts).run();
	}
}
