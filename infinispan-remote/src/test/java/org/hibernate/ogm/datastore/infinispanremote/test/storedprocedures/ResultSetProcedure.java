/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispanremote.test.storedprocedures;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.ogm.backendtck.storedprocedures.Car;

import org.infinispan.Cache;
import org.infinispan.tasks.ServerTask;
import org.infinispan.tasks.TaskContext;

/**
 * @author The Viet Nguyen
 */
public class ResultSetProcedure implements ServerTask<List<Car>> {

	private Cache<Object, Object> cache;

	private int id;
	private String title;

	@SuppressWarnings( "unchecked" )
	@Override
	public void setTaskContext(TaskContext taskContext) {
		cache = (Cache<Object, Object>) taskContext.getCache()
				.orElseThrow( () -> new RuntimeException( "missing cache" ) );
		id = taskContext.getParameters()
				.map( p -> p.get( "id" ) )
				.map( Integer.class::cast )
				.orElseThrow( () -> new RuntimeException( "missing parameter 'id'" ) );
		title = taskContext.getParameters()
				.map( p -> p.get( "title" ) )
				.map( String.class::cast )
				.orElseThrow( () -> new RuntimeException( "missing parameter 'title'" ) );
	}

	@Override
	public List<Car> call() {
		cache.clear();
		cache.put( id, title );
		return cache.entrySet()
				.stream()
				.filter( e -> Objects.equals( e.getKey(), id ) )
				.limit( 1 )
				.map( e -> new Car( ( (int) e.getKey() ), ( (String) e.getValue() ) ) )
				.collect( Collectors.toList() );
	}

	@Override
	public String getName() {
		return "resultSetProcedure";
	}
}
