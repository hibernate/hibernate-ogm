/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.cfg.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.ogm.util.impl.Contracts;

/**
 * Represents one or several hosts with their ports.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class Hosts implements Iterable<Hosts.HostAndPort> {
	// explicit new instance. A tiny bit safer during comparison
	public static final Hosts NO_HOST = new Hosts( Collections.<String>emptyList(), Collections.<Integer>emptyList() );
	private static final Pattern LIKELY_IPV6 = Pattern.compile( "^[\\d\\.:a-fA-F]$" );
	private static final String COMMA = ", ";

	private List<HostAndPort> hostsAndPorts;

	public Hosts(List<String> hosts, List<Integer> ports) {
		int length = hosts.size();
		Contracts.assertTrue( length == ports.size(), "Not the same number of hosts and ports" );
		this.hostsAndPorts = new ArrayList<HostAndPort>( length );
		for ( int i = 0 ; i < length; i++ ) {
			hostsAndPorts.add( new HostAndPort( hosts.get( i ), ports.get( i ) ) );
		}
	}

	public boolean hasHost() {
		return hostsAndPorts.size() != 0;
	}

	public boolean isSingleHost() {
		return hostsAndPorts.size() == 1;
	}

	public int size() {
		return hostsAndPorts.size();
	}

	public HostAndPort getFirst() {
		Contracts.assertTrue( hasHost(), "getFirst called on an empty Hosts iterator" );
		return hostsAndPorts.get( 0 );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for ( HostAndPort hostAndPort : hostsAndPorts ) {
			sb.append( COMMA );
			String host = hostAndPort.getHost();
			// add square brackets for IPv6
			boolean matches = LIKELY_IPV6.matcher( host ).matches();
			if ( matches ) {
				sb.append( "[" );
			}
			sb.append( hostAndPort.getHost() );
			if ( matches ) {
				sb.append( "]" );
			}
			sb.append( ":" ).append( hostAndPort.getPort() );
		}
		String string = sb.toString();
		if ( string.startsWith( COMMA ) ) {
			return string.substring( COMMA.length() );
		}
		return string;
	}

	@Override
	public Iterator<HostAndPort> iterator() {
		return hostsAndPorts.iterator();
	}

	public static class HostAndPort {

		private String host;
		private Integer port;

		public HostAndPort(String host, Integer port) {
			Contracts.assertNotNull( host, "host" );
			Contracts.assertNotNull( host, "port" );
			this.host = host;
			this.port = port;
		}

		public String getHost() {
			return host;
		}

		public Integer getPort() {
			return port;
		}
	}
}
