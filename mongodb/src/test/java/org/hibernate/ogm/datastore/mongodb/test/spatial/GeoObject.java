/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.spatial;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.ogm.datastore.mongodb.type.GeoLineString;
import org.hibernate.ogm.datastore.mongodb.type.GeoMultiLineString;
import org.hibernate.ogm.datastore.mongodb.type.GeoMultiPoint;
import org.hibernate.ogm.datastore.mongodb.type.GeoMultiPolygon;
import org.hibernate.ogm.datastore.mongodb.type.GeoPoint;
import org.hibernate.ogm.datastore.mongodb.type.GeoPolygon;
import org.hibernate.ogm.options.shared.IndexOption;
import org.hibernate.ogm.options.shared.IndexOptions;

@Entity
@Table(name = GeoObject.COLLECTION_NAME, indexes = {
		@Index(columnList = "point", name = "point_spatial_idx"),
		@Index(columnList = "multiPoint", name = "multiPoint_spatial_idx"),
		@Index(columnList = "lineString", name = "lineString_spatial_idx"),
		@Index(columnList = "multiLineString", name = "multiLineString_spatial_idx"),
		@Index(columnList = "polygon", name = "polygon_spatial_idx"),
		@Index(columnList = "multiPolygon", name = "multiPolygon_spatial_idx")
})
@IndexOptions({
		@IndexOption(forIndex = "point_spatial_idx", options = "{ _type: '2dsphere' }"),
		@IndexOption(forIndex = "multiPoint_spatial_idx", options = "{ _type: '2dsphere' }"),
		@IndexOption(forIndex = "lineString_spatial_idx", options = "{ _type: '2dsphere' }"),
		@IndexOption(forIndex = "multiLineString_spatial_idx", options = "{ _type: '2dsphere' }"),
		@IndexOption(forIndex = "polygon_spatial_idx", options = "{ _type: '2dsphere' }"),
		@IndexOption(forIndex = "multiPolygon_spatial_idx", options = "{ _type: '2dsphere' }")
})
public class GeoObject {

	public static final String COLLECTION_NAME = "T_GEO_OBJECT";

	@Id
	private Long id;

	private GeoPoint point;

	private GeoMultiPoint multiPoint;

	private GeoLineString lineString;

	private GeoMultiLineString multiLineString;

	private GeoPolygon polygon;

	private GeoMultiPolygon multiPolygon;

	public GeoObject() {
	}

	public GeoObject(Long id,
			GeoPoint point,
			GeoMultiPoint multiPoint,
			GeoLineString lineString,
			GeoMultiLineString multiLineString,
			GeoPolygon polygon,
			GeoMultiPolygon multiPolygon) {
		this.id = id;
		this.point = point;
		this.multiPoint = multiPoint;
		this.lineString = lineString;
		this.multiLineString = multiLineString;
		this.polygon = polygon;
		this.multiPolygon = multiPolygon;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public GeoPoint getPoint() {
		return point;
	}

	public void setPoint(GeoPoint point) {
		this.point = point;
	}

	public GeoMultiPoint getMultiPoint() {
		return multiPoint;
	}

	public void setMultiPoint(GeoMultiPoint multiPoint) {
		this.multiPoint = multiPoint;
	}

	public GeoLineString getLineString() {
		return lineString;
	}

	public void setLineString(GeoLineString lineString) {
		this.lineString = lineString;
	}

	public GeoMultiLineString getMultiLineString() {
		return multiLineString;
	}

	public void setMultiLineString(GeoMultiLineString multiLineString) {
		this.multiLineString = multiLineString;
	}

	public GeoPolygon getPolygon() {
		return polygon;
	}

	public void setPolygon(GeoPolygon polygon) {
		this.polygon = polygon;
	}

	public GeoMultiPolygon getMultiPolygon() {
		return multiPolygon;
	}

	public void setMultiPolygon(GeoMultiPolygon multiPolygon) {
		this.multiPolygon = multiPolygon;
	}

	@Override
	public String toString() {
		return "GeoObject [id=" + id + "]";
	}
}
