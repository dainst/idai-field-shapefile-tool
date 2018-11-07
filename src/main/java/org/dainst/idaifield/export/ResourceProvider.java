package org.dainst.idaifield.export;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dainst.idaifield.model.Geometry;
import org.dainst.idaifield.model.GeometryType;
import org.dainst.idaifield.model.Resource;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Thomas Kleinke
 */
class ResourceProvider {

    static Map<GeometryType, List<Resource>> getResources(String projectName) throws Exception {

        List<Resource> resources = extractResources(getJsonData(projectName));

        return getResourcesMap(resources);
    }


    private static JSONArray getJsonData(String projectName) throws Exception {

        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            HttpPost httpPost = new HttpPost("http://localhost:3000/" + projectName + "/_find");
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpPost.setHeader(HttpHeaders.ACCEPT, "application/json");
            httpPost.setEntity(new StringEntity("{ \"selector\": { \"resource.geometry\": { \"$gt\": null } } }"));

            CloseableHttpResponse response = httpClient.execute(httpPost);

            try {
                JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
                return json.getJSONArray("docs");
            } finally {
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }


    private static List<Resource> extractResources(JSONArray jsonData) throws Exception {

        List<Resource> resources = new ArrayList<>();

        for (int i = 0; i < jsonData.length(); i++) {
            JSONObject jsonResource = jsonData.getJSONObject(i).getJSONObject("resource");
            resources.add(createResource(jsonResource));
        }

        return resources;
    }


    private static Resource createResource(JSONObject jsonResource) throws Exception {

        Resource resource = new Resource();
        resource.setId(jsonResource.getString("id"));
        resource.setIdentifier(jsonResource.getString("identifier"));
        resource.setType(jsonResource.getString("type"));
        if (jsonResource.has("shortDescription")) {
            resource.setShortDescription(jsonResource.getString("shortDescription"));
        }


        JSONObject jsonGeometry = jsonResource.getJSONObject("geometry");
        resource.setGeometry(createGeometry(jsonGeometry));

        return resource;
    }


    private static Geometry createGeometry(JSONObject jsonGeometry) throws Exception {

        Geometry geometry = new Geometry();
        JSONArray jsonCoordinates = jsonGeometry.getJSONArray("coordinates");

        switch(jsonGeometry.getString("type")) {
            case "Point":
                geometry.setType(GeometryType.MULTIPOINT);
                geometry.setCoordinates(new double[][][][]{{{extractPointCoordinates(jsonCoordinates)}}});
                break;
            case "MultiPoint":
                geometry.setType(GeometryType.MULTIPOINT);
                geometry.setCoordinates(new double[][][][]{{extractMultiPointOrPolylineCoordinates(jsonCoordinates)}});
                break;
            case "LineString":
                geometry.setType(GeometryType.MULTIPOLYLINE);
                geometry.setCoordinates(new double[][][][]{{extractMultiPointOrPolylineCoordinates(jsonCoordinates)}});
                break;
            case "MultiLineString":
                geometry.setType(GeometryType.MULTIPOLYLINE);
                geometry.setCoordinates(new double[][][][]{extractMultiPolylineOrPolygonCoordinates(jsonCoordinates)});
                break;
            case "Polygon":
                geometry.setType(GeometryType.MULTIPOLYGON);
                geometry.setCoordinates(new double[][][][]{extractMultiPolylineOrPolygonCoordinates(jsonCoordinates)});
                break;
            case "MultiPolygon":
                geometry.setType(GeometryType.MULTIPOLYGON);
                geometry.setCoordinates(extractMultiPolygonCoordinates(jsonCoordinates));
                break;
            default:
                throw new Exception("Invalid geometry type: " + jsonGeometry.getString("type"));
        }

        return geometry;
    }


    private static double[] extractPointCoordinates(JSONArray jsonCoordinates) {

        return new double[]{jsonCoordinates.getDouble(0), jsonCoordinates.getDouble(1)};
    }


    private static double[][] extractMultiPointOrPolylineCoordinates(JSONArray jsonCoordinates) {

        double[][] coordinates = new double[jsonCoordinates.length()][2];
        for (int i = 0; i < jsonCoordinates.length(); i++) {
            coordinates[i] = extractPointCoordinates(jsonCoordinates.getJSONArray(i));
        }

        return coordinates;
    }


    private static double[][][] extractMultiPolylineOrPolygonCoordinates(JSONArray jsonCoordinates) {

        double[][][] coordinates = new double[jsonCoordinates.length()][][];

        for (int i = 0; i < jsonCoordinates.length(); i++) {
            coordinates[i] = extractMultiPointOrPolylineCoordinates(jsonCoordinates.getJSONArray(i));
        }

        return coordinates;
    }


    private static double[][][][] extractMultiPolygonCoordinates(JSONArray jsonCoordinates) {

        double [][][][] coordinates = new double[jsonCoordinates.length()][][][];

        for (int i = 0; i < jsonCoordinates.length(); i ++) {
            coordinates[i] = extractMultiPolylineOrPolygonCoordinates(jsonCoordinates.getJSONArray(i));
        }

        return coordinates;
    }


    private static Map<GeometryType, List<Resource>> getResourcesMap(List<Resource> resources) {

        Map<GeometryType, List<Resource>> resourcesMap = new HashMap<>();

        for (Resource resource : resources) {
            GeometryType geometryType = resource.getGeometry().getType();
            if (!resourcesMap.containsKey(geometryType)) {
                resourcesMap.put(geometryType, new ArrayList<>());
            }
            resourcesMap.get(geometryType).add(resource);
        }

        return resourcesMap;
    }
}
