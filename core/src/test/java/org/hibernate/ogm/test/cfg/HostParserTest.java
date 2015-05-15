/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.cfg;

import java.util.Iterator;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

import org.junit.Test;

import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.impl.HostParser;
import org.hibernate.ogm.cfg.spi.Hosts;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class HostParserTest {
	private static Integer EXPLICIT_GLOBAL_PORT = 456;
	private static Integer DEFAULT_PORT = 789;

	@Test
	public void testNullHostParsing() throws Exception {
		assertThat( HostParser.parse( null, EXPLICIT_GLOBAL_PORT, DEFAULT_PORT ) == Hosts.NO_HOST ).isTrue();
		assertThat( HostParser.parse( "", EXPLICIT_GLOBAL_PORT, DEFAULT_PORT ) == Hosts.NO_HOST ).isTrue();
		assertThat( HostParser.parse( " ", EXPLICIT_GLOBAL_PORT, DEFAULT_PORT ) == Hosts.NO_HOST ).isTrue();
		assertThat( Hosts.NO_HOST.hasHost() ).isFalse();
		assertThat( Hosts.NO_HOST.isSingleHost() ).isFalse();
	}

	@Test
	public void testSingleHostParsing() throws Exception {
		checkSingleHost( "www.example.com", "www.example.com", EXPLICIT_GLOBAL_PORT );
		checkSingleHost( "www.example.com:123", "www.example.com", 123 );
		checkSingleHost( " www.example.com ", "www.example.com", EXPLICIT_GLOBAL_PORT );
		checkSingleHost( " www.example.com:123 ", "www.example.com", 123 );
		checkSingleHost( "192.0.2.1", "192.0.2.1", EXPLICIT_GLOBAL_PORT );
		checkSingleHost( "192.0.2.1:123", "192.0.2.1", 123 );
		checkSingleHost( "2001:db8::ff00:42:8329", "2001:db8::ff00:42:8329", EXPLICIT_GLOBAL_PORT );
		checkSingleHost( "::ffff:192.0.2.128", "::ffff:192.0.2.128", EXPLICIT_GLOBAL_PORT );
		checkSingleHost( "[2001:db8::ff00:42:8329]:123", "2001:db8::ff00:42:8329", 123 );
	}

	@Test
	public void testMultipleHostParsing() throws Exception {
		Hosts results = HostParser.parse(
				"www.example.com, www2.example.com:123, 192.0.2.1, 192.0.2.2:123, 2001:db8::ff00:42:8329, [2001:db8::ff00:42:8329]:123",
				EXPLICIT_GLOBAL_PORT,
				DEFAULT_PORT
		);
		assertThat( results.isSingleHost() ).isFalse();
		assertThat( results.hasHost() ).isTrue();
		Iterator<Hosts.HostAndPort> hostAndPortIterator = results.iterator();

		assertThat( hostAndPortIterator.hasNext() ).isTrue();
		Hosts.HostAndPort hostAndPort = hostAndPortIterator.next();
		assertThat( hostAndPort.getHost() ).isEqualTo( "www.example.com" );
		assertThat( hostAndPort.getPort() ).isEqualTo( DEFAULT_PORT );

		assertThat( hostAndPortIterator.hasNext() ).isTrue();
		hostAndPort = hostAndPortIterator.next();
		assertThat( hostAndPort.getHost() ).isEqualTo( "www2.example.com" );
		assertThat( hostAndPort.getPort() ).isEqualTo( 123 );

		assertThat( hostAndPortIterator.hasNext() ).isTrue();
		hostAndPort = hostAndPortIterator.next();
		assertThat( hostAndPort.getHost() ).isEqualTo( "192.0.2.1" );
		assertThat( hostAndPort.getPort() ).isEqualTo( DEFAULT_PORT );

		assertThat( hostAndPortIterator.hasNext() ).isTrue();
		hostAndPort = hostAndPortIterator.next();
		assertThat( hostAndPort.getHost() ).isEqualTo( "192.0.2.2" );
		assertThat( hostAndPort.getPort() ).isEqualTo( 123 );

		assertThat( hostAndPortIterator.hasNext() ).isTrue();
		hostAndPort = hostAndPortIterator.next();
		assertThat( hostAndPort.getHost() ).isEqualTo( "2001:db8::ff00:42:8329" );
		assertThat( hostAndPort.getPort() ).isEqualTo( DEFAULT_PORT );

		assertThat( hostAndPortIterator.hasNext() ).isTrue();
		hostAndPort = hostAndPortIterator.next();
		assertThat( hostAndPort.getHost() ).isEqualTo( "2001:db8::ff00:42:8329" );
		assertThat( hostAndPort.getPort() ).isEqualTo( 123 );

		assertThat( hostAndPortIterator.hasNext() ).isFalse();
	}

	@Test
	public void testIncorrectHosts() throws Exception {
		checkIncorrectHost( "www example.com" );
		checkIncorrectHost( "www:example.com" );
		checkIncorrectHost( "www:example.com:123" );
		checkIncorrectHost( "www.example.com :123" );
		checkIncorrectHost( "www.example.com:notnumber" );
		checkIncorrectHost( "[www.example.com]" );
		checkIncorrectHost( "[2001:db8::ff00:42:8329:123" );
		checkIncorrectHost( "2001:db8::zz00:42:8329" );
	}

	private void checkIncorrectHost(String hostString) {
		try {
			HostParser.parse( hostString, EXPLICIT_GLOBAL_PORT, DEFAULT_PORT );
			fail( hostString + " should be an incorrect host" );
		}
		catch (HibernateException e) {
			assertThat( e.getMessage() ).startsWith( "OGM000079" );
		}
	}

	private void checkSingleHost(String hostString, String host, Integer port) {
		Hosts result = HostParser.parse( hostString, EXPLICIT_GLOBAL_PORT, DEFAULT_PORT );
		assertThat( result.hasHost() ).isTrue();
		assertThat( result.isSingleHost() ).isTrue();
		Iterator<Hosts.HostAndPort> iterator = result.iterator();
		Hosts.HostAndPort hostAndPort = iterator.next();
		assertThat( iterator.hasNext() ).isFalse();
		assertThat( hostAndPort.getHost() ).isEqualTo( host );
		assertThat( hostAndPort.getPort() ).isEqualTo( port );
	}
}
