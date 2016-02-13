/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.utils;

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chernolyassv
 */
@Deprecated
public class MemoryDBUtil {

	private static final Logger LOG = Logger.getLogger( MemoryDBUtil.class.getName() );

	public static Map<String, List<ORecordId>> prepareDb(String url) {

		Map<String, List<ORecordId>> idMap = new HashMap<>();
		try {
			// System.setProperty("ORIENTDB_HOME", "./target");
			OrientGraphFactory factory = new OrientGraphFactory( url );

			OrientGraphNoTx graph = factory.getNoTx();

			// vertex classes
			OrientVertexType pizzaType = graph.createVertexType( "Pizza" );
			pizzaType.createProperty( "name", OType.STRING );
			for ( OProperty p : pizzaType.declaredProperties() ) {
				System.out.println( "Property: " + p );
			}

			OrientVertexType buyingOrderType = graph.createVertexType( "BuyingOrder" );
			buyingOrderType.createProperty( "orderKey", OType.STRING );
			OrientVertexType orderItemType = graph.createVertexType( "OrderItem" );
			orderItemType.createProperty( "cost", OType.DECIMAL );

			OrientVertexType customerType = graph.createVertexType( "Customer" );
			customerType.createProperty( "name", OType.STRING );

			// create vertex
			Vertex pizza = graph.addVertex( "class:Pizza" );
			pizza.setProperty( "name", "Super Papa" );
			System.out.println( "pizza.getId():" + pizza.getId() );
			List<ORecordId> pizzaRids = new LinkedList<>();
			pizzaRids.add( (ORecordId) pizza.getId() );
			idMap.put( "Pizza", pizzaRids );

			Vertex customer = graph.addVertex( "class:Customer" );
			customer.setProperty( "name", "Ivahoe" );
			System.out.println( "customer.getId():" + customer.getId() );
			List<ORecordId> customerRids = new LinkedList<>();
			customerRids.add( (ORecordId) customer.getId() );
			idMap.put( "Customer", customerRids );

			Vertex buyingOrder = graph.addVertex( "class:BuyingOrder" );
			buyingOrder.setProperty( "orderKey", "2233" );
			System.out.println( "order.getId():" + buyingOrder.getId() );
			List<ORecordId> orderRids = new LinkedList<>();
			orderRids.add( (ORecordId) buyingOrder.getId() );
			idMap.put( "BuyingOrder", orderRids );

			Vertex orderItem = graph.addVertex( "class:OrderItem" );
			orderItem.setProperty( "cost", new BigDecimal( "10.2" ) );
			System.out.println( "orderItem.getId():" + orderItem.getId() );
			List<ORecordId> orderItemRids = new LinkedList<>();
			orderItemRids.add( (ORecordId) orderItem.getId() );
			idMap.put( "OrderItem", orderItemRids );

			// create edges
			graph.addEdge( null, pizza, orderItem, "Buying" );
			graph.addEdge( null, buyingOrder, orderItem, "Order" );
			graph.addEdge( null, customer, buyingOrder, "Owner" );

		}
		catch (Exception e) {
			LOG.log( Level.SEVERE, "Error", e );
		}
		return idMap;
	}

}
