/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.json.impl;

import java.util.List;
import java.util.Map;

/**
 * Results of a {@link Statement} expressed as graph elements.
 *
 * @author Davide D'Alto
 */
public class Graph {

	private List<Node> nodes;

	private List<Relationship> relationships;

	public List<Node> getNodes() {
		return nodes;
	}

	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}

	public List<Relationship> getRelationships() {
		return relationships;
	}

	public void setRelationships(List<Relationship> relationships) {
		this.relationships = relationships;
	}

	public static class Node {

		private Long id;
		private List<String> labels;
		private Map<String, Object> properties;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<String> getLabels() {
			return labels;
		}

		public void setLabels(List<String> labels) {
			this.labels = labels;
		}

		public Map<String, Object> getProperties() {
			return properties;
		}

		public void setProperties(Map<String, Object> properties) {
			this.properties = properties;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			Node other = (Node) obj;
			if ( id == null ) {
				if ( other.id != null ) {
					return false;
				}
			}
			else if ( !id.equals( other.id ) ) {
				return false;
			}
			return true;
		}
	}

	public static class Relationship {

		private Long id;
		private String type;
		private Long startNode;
		private Long endNode;
		private Map<String, Object> properties;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Long getStartNode() {
			return startNode;
		}

		public void setStartNode(Long startNode) {
			this.startNode = startNode;
		}

		public Long getEndNode() {
			return endNode;
		}

		public void setEndNode(Long endNode) {
			this.endNode = endNode;
		}

		public Map<String, Object> getProperties() {
			return properties;
		}

		public void setProperties(Map<String, Object> properties) {
			this.properties = properties;
		}
	}
}
