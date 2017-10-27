/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.cfg.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.ogm.cfg.spi.Hosts;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Parser for the host property.
 * Could use parboiled if things become more complicated but so far the code is more compact.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class HostParser {
	/* Allows: host, host:port, ipv4, ipv4:port, ipv6, [ipv6]:port
	 * regexp: ^([^\[\]:\s]+|\[[\da-fA-F:\.]+\]|[\da-fA-F:\.]+)(:(\d+))*$
	 * ^ // start from the beginning
	 * ( // first group
	 *   [^\[\]:\s]+ // allow everything but square brackets, colon and whitespace. At least one element: IPv4 and host
	 *   | // or
	 *   \[[\da-fA-F:\.]+\] // allow numbers, a->f and colon within a bracket, at least one element: IPv6 [12:31:211:2f::]
	 *   | // or
	 *   [\da-fA-F:\.]+ // allow numbers, a->f and colon, at least one element: IPv6  12:31:211:2f::
	 * ) // end of first group
	 * ( // second group
	 *   :(\d+) // colon and digits, also defines the third group
	 * )* //end of second group, optional
	 * $ // end marker
	 */
	private static final Pattern HOST_AND_PORT_PATTERN = Pattern.compile( "^([^\\[\\]:\\s]+|\\[[\\da-fA-F:\\.]+\\]|[\\da-fA-F:\\.]+)(:(\\d+))*$" );

	// remove surrounding square brackets
	// regex: ^\[(.+)\]$
	private static final Pattern NAKED_IPV6_PATTERN = Pattern.compile( "^\\[(.+)\\]$" );

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	/**
	 * Accepts a comma separated list of host / ports.
	 *
	 * For example
	 *
	 * www.example.com, www2.example.com:123, 192.0.2.1, 192.0.2.2:123, 2001:db8::ff00:42:8329, [2001:db8::ff00:42:8329]:123
	 */
	public static Hosts parse(String hostString, Integer explicitGlobalPort, Integer defaultPort) {
		List<String> hosts = new ArrayList<>();
		List<Integer> ports = new ArrayList<>();
		if ( hostString == null || hostString.trim().isEmpty() ) {
			return Hosts.NO_HOST;
		}
		// for each element between commas
		String[] splits = hostString.split( "," );
		for ( String rawSplit : splits ) {
			// remove whitespaces
			String split = rawSplit.trim();
			//
			Matcher matcher = HOST_AND_PORT_PATTERN.matcher( split );
			if ( matcher.matches() ) {
				setCleanHost( matcher, hosts );
				setPort( ports, matcher, explicitGlobalPort, splits, defaultPort );
			}
			else {
				throw LOG.unableToParseHost( hostString );
			}
		}
		return new Hosts( hosts, ports );
	}

	private static void setPort(List<Integer> ports, Matcher matcher, Integer globalPort, String[] splits, Integer defaultPort) {
		String portAsString = matcher.group( 3 );
		// get the port or if null add the null port
		if ( portAsString != null ) {
			ports.add( Integer.valueOf( portAsString ) );
		}
		else if ( splits.length == 1 && globalPort != null ) {
			// single host and the property OgmProperties.PORT is set
			// this is the legacy setting
			ports.add( globalPort );
		}
		else {
			ports.add( defaultPort );
		}
	}

	/**
	 * If host is of the form [ipv6], return ipv6
	 */
	private static void setCleanHost(Matcher matcher, List<String> hosts) {
		// get the host but might be [ipv6]
		String maybeIPv6Host = matcher.group( 1 );
		Matcher ipv6Matcher = NAKED_IPV6_PATTERN.matcher( maybeIPv6Host );
		String cleanHost = maybeIPv6Host;
		if ( ipv6Matcher.matches() ) {
			cleanHost = ipv6Matcher.group( 1 );
			if ( cleanHost == null ) {
				cleanHost = maybeIPv6Host;
			}
		}
		hosts.add( cleanHost );
	}
}
